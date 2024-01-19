create table journalfoering(
    id bigserial primary key,
    navident varchar(7) not null,
    foedselsnummer varchar(11) not null,
    opprettet_tidspunkt timestamp with time zone not null default current_timestamp
);