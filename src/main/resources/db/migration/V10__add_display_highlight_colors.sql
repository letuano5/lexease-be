alter table display_settings
    add column highlight_background_color text not null default '#FEF08A',
    add column highlight_text_color text not null default '#111111';
