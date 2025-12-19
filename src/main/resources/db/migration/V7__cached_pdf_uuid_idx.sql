ALTER TABLE cachet_pdf
    ADD CONSTRAINT uq_cachet_pdf_uuid UNIQUE (uuid);

create index cached_pdf_uuid_idx on cachet_pdf(uuid);