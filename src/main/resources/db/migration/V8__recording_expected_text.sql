alter table recordings
    add column expected_text text not null default '';

alter table recordings
    alter column expected_text drop default;
