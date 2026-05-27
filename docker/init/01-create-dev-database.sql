SELECT 'CREATE DATABASE smartops_dev'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'smartops_dev'
)\gexec
