alter table recordings
    add column audio_object_key text not null default '',
    add column audio_mime_type text not null default 'application/octet-stream';

alter table recordings
    alter column audio_object_key drop default,
    alter column audio_mime_type drop default;

drop table if exists recording_words;
