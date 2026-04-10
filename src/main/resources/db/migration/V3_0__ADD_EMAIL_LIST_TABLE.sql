drop table if exists saved_emails;

CREATE TABLE saved_emails(
    id SERIAL PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    saved_email VARCHAR NOT NULL,
    CONSTRAINT saved_emails_user_id_saved_email_key UNIQUE (user_id, saved_email)
);