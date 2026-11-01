create table event_outbox (
    id uuid primary key,
    event_type varchar(80) not null,
    aggregate_id varchar(120) not null,
    payload text not null,
    occurred_at timestamptz not null,
    available_at timestamptz not null,
    locked_until timestamptz,
    attempts integer not null default 0,
    published_at timestamptz,
    last_error varchar(1000)
);

create index event_outbox_dispatch_idx
    on event_outbox (available_at, occurred_at)
    where published_at is null;

create table impact_events (
    event_id uuid primary key,
    event_type varchar(80) not null,
    aggregate_id varchar(120) not null,
    occurred_at timestamptz not null
);

create index impact_events_type_time_idx on impact_events (event_type, occurred_at);
