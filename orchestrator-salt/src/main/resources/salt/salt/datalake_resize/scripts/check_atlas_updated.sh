#!/bin/bash

LOGFILE=/var/log/dl_resize_check_atlas_updated.log

doLog() {
    echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $1" >>$LOGFILE
}

init() {
  # Determine Atlas keytab path.
  ATLAS_KT=$(find / -wholename "*atlas-ATLAS_SERVER/atlas.keytab" 2>/dev/null | head -n 1)

  # Setup required configuration files if needed.
  if [[ ! -f jaas.conf ]]; then
      ATLAS_PRINCIPAL=$(klist -kt "${ATLAS_KT}" | grep -o -m 1 "atlas\/\S*")
      printf "KafkaClient {
      \tcom.sun.security.auth.module.Krb5LoginModule required
      \tuseKeyTab=true
      \tkeyTab=\"%s\"
      \tprincipal=\"%s\";\n};\n" "${ATLAS_KT}" "${ATLAS_PRINCIPAL}" > jaas.conf
  fi

  if [[ ! -f client.config ]]; then
      printf "security.protocol=SASL_SSL\nsasl.kerberos.service.name=kafka\n" > client.config
  fi

  # Determine the Kafka bootstrap server to use.
  KAFKA_SERVER=$(grep --line-buffered -oP "atlas.kafka.bootstrap.servers=\K.*" \
      /etc/atlas/conf/atlas-application.properties | awk -F',' '{print $1}')

  # Export Kafka-specific environment variables.
  export KAFKA_HEAP_OPTS="-Xms512m -Xmx1g"
  export KAFKA_OPTS="-Djava.security.auth.login.config=${PWD}/jaas.conf"

  # Kinit into Atlas keytab as Atlas user.
  kinit -kt "$ATLAS_KT" "atlas/$(hostname -f)" 2>/dev/null
}

check_atlas_lineage() {
  # Obtain Atlas lineage information.
  LINEAGE_INFO=$(/opt/cloudera/parcels/CDH/lib/kafka/bin/kafka-consumer-groups.sh \
      --bootstrap-server "${KAFKA_SERVER}" --describe --group atlas \
      --command-config="${PWD}/client.config" 2>/dev/null \
      | awk '{print $2, $6}')

  if [[ -z "$LINEAGE_INFO" ]]; then
      doLog "*ERROR*: Unable to get lineage info for Atlas. Please look at the created configuration files to make sure they look correct."
      exit 1
  fi

  # Parse lineage information and determine if Atlas is out of date.
  LINEAGE_LAG_VALS=($LINEAGE_INFO)
  NUM_LAG_VALS=${#LINEAGE_LAG_VALS[@]}
  OUT_OF_DATE_TOPICS=""
  for (( i = 2; i < ${NUM_LAG_VALS}; i += 2 )); do
      if [[ ${LINEAGE_LAG_VALS[${i} + 1]} != '-' && ${LINEAGE_LAG_VALS[${i} + 1]} != '0' ]]; then
          OUT_OF_DATE_TOPICS="${OUT_OF_DATE_TOPICS}${LINEAGE_LAG_VALS[$i]}, "
      fi
  done

  if [[ -z "$OUT_OF_DATE_TOPICS" ]]; then
      doLog "Atlas is up to date!"
      return 0
  else
      doLog "The following Atlas topics are not up to date: ${OUT_OF_DATE_TOPICS%??}!"
      doLog "Waiting some more for Atlas to be entirely up to date before continuing with the migration."
      return 1
  fi
}

doLog "Starting the waiting process for Atlas being fully up to date in order to proceed with DL resize."
init()
MAX_RETRIES=$1
j=0
while [[ "$j" -lt "$MAX_RETRIES" ]] ; do
  if [[ "$(check_atlas_lineage)" == "0" ]]; then
    doLog "Finished waiting for Atlas to be up to date. Proceeding with DL resize."
    exit 0
  fi
  ((j++))
  sleep 5
done

doLog "Timed out while waiting for Atlas to be up to date."
exit 1
