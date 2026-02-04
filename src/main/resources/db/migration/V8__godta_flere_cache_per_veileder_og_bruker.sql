alter table cachet_pdf drop constraint cachet_pdf_pkey;
alter table cachet_pdf add constraint uuid_pkey primary key (uuid);
