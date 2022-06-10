/opt/salt/scripts/check_atlas_updated.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://datalake_resize/scripts/check_atlas_updated.sh
    - template: jinja
