create table ai_intake_suggestions (
    id uuid primary key,
    donor_id varchar(120) not null,
    input_text varchar(4000) not null,
    suggested_payload text not null,
    model_id varchar(120) not null,
    prompt_version varchar(40) not null,
    status varchar(24) not null,
    created_at timestamptz not null
);

create index ai_intake_suggestions_donor_time_idx
    on ai_intake_suggestions (donor_id, created_at desc);
