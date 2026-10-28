create table donations (
    id uuid primary key,
    donor_id varchar(120) not null,
    title varchar(160) not null,
    description varchar(1000) not null,
    quantity integer not null check (quantity > 0),
    unit varchar(20) not null,
    available_until timestamptz not null,
    pickup_latitude double precision not null check (pickup_latitude between -90 and 90),
    pickup_longitude double precision not null check (pickup_longitude between -180 and 180),
    status varchar(20) not null,
    created_at timestamptz not null,
    version bigint not null default 0
);

create index donations_available_expiry_idx on donations (status, available_until);
create index donations_geo_idx on donations (pickup_latitude, pickup_longitude) where status = 'AVAILABLE';

create table donation_dietary_tags (
    donation_id uuid not null references donations(id) on delete cascade,
    tag varchar(40) not null,
    primary key (donation_id, tag)
);

create table recipients (
    id varchar(120) primary key,
    name varchar(160) not null,
    latitude double precision not null check (latitude between -90 and 90),
    longitude double precision not null check (longitude between -180 and 180),
    daily_capacity integer not null check (daily_capacity > 0),
    created_at timestamptz not null
);

create table recipient_dietary_tags (
    recipient_id varchar(120) not null references recipients(id) on delete cascade,
    tag varchar(40) not null,
    primary key (recipient_id, tag)
);

create table claims (
    id uuid primary key,
    donation_id uuid not null references donations(id),
    recipient_id varchar(120) not null references recipients(id),
    idempotency_key varchar(128) unique,
    status varchar(20) not null,
    reserved_at timestamptz not null,
    reserved_until timestamptz not null,
    completed_at timestamptz,
    version bigint not null default 0
);

create index claims_recipient_day_idx on claims (recipient_id, reserved_at, status);
create index claims_reservation_expiry_idx on claims (status, reserved_until);

