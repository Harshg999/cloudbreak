include:
  - datalake_resize.check_atlas_updated

check_atlas_updated:
  cmd.run:
    - name: /opt/salt/scripts/check_atlas_updated.sh {{salt['pillar.get']('check_atlas_updated:max_retries')}}
    - require:
      - sls: datalake_resize.check_atlas_updated
