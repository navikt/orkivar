create table journalfoeringer(
    id bigserial primary key,
    referanse uuid unique not null,
    journalpost_id TEXT unique not null,
    navident varchar(7) not null,
    foedselsnummer varchar(11) not null,
    opprettet_tidspunkt timestamp with time zone not null default current_timestamp
);