update journalfoeringer
set type = 'JOURNALFØRING'
where type is null;

alter table journalfoeringer alter column type set not null;