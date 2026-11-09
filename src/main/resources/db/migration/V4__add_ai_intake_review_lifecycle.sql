alter table ai_intake_suggestions
    add column reviewed_at timestamptz,
    add column donation_id uuid references donations(id);

create index ai_intake_suggestions_status_time_idx
    on ai_intake_suggestions (status, created_at desc);
