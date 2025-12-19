create table cachet_pdf (
    fnr TEXT NOT NULL,
    veileder_ident TEXT NOT NULL,
    pdf BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY (veileder_ident, fnr)
)