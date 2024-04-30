create table users (
    id          varchar(36) not null,
    email       varchar(255) not null unique,
    first_name  varchar(255) not null,
    last_name   varchar(255) not null,
    password_hash  varchar(255) not null,
    username    varchar(255) not null,
    dob         datetime not null,
    reminders    boolean,
    `description`  text,
    profile_pic varchar(255),
    primary key (id)
);

create table auth_token (
    id          varchar(36) not null,
    user_id     varchar(36) not null,
    issue_time  datetime not null,
    expiry_time datetime not null,

    primary key (id),
    foreign key (user_id) references users(id)
);

create table locations (
    id          varchar(36) not null,
    street_no    int,
    unit_no      varchar(255),
    street_name  varchar(255),
    suburb      varchar(255),
    postcode    varchar(255) not null,
    state       char(255) not null,
    country     char(255) not null,
    longitude   double,
    latitude    double,
    primary key (id)
);

create table `events` (
    id          varchar(36) not null,
    host_id      varchar(36) not null,
    location_id  varchar(36),
    event_name   varchar(255) not null,
    event_start       datetime not null,
    event_end         datetime not null,
    event_description text,
    seat_availability int,
    seat_capacity int,
    event_pic varchar(255) not null,
    published boolean not null,
    spotify_playlist  varchar(255),
/*    has_seats    boolean not null,*/
    primary key (id),
    foreign key (host_id) references users(id),
    foreign key (location_id) references locations(id)
);

create table admins (
    event_id     varchar(36) not null,
    user_id      varchar(36) not null,
    primary key (event_id, user_id),
    foreign key (event_id) references `events` (id),
    foreign key (user_id) references users(id)
);

create table `user_groups` (
    id          varchar(36) not null,
    leader_id    varchar(36) not null,
    size        int not null,
    time_created datetime,
    ticket_available int,
    primary key (id),
    foreign key (leader_id) references users(id)
);

create table notification_members (
    event_id     varchar(36) not null,
    user_id      varchar(36) not null,
    primary key (event_id, user_id),
    foreign key (event_id) references `events` (id),
    foreign key (user_id) references users(id)     
);

create table seating_plan (
    id          varchar(36) not null,
    event_id     varchar(36) not null,
    location_id  varchar(36) not null,
    section     varchar(255) not null,
    seat_availability int not null,
    total_seats int not null,
    ticket_price float not null,
    has_seats boolean not null,
    primary key (id),
    foreign key (event_id) references `events`(id)
);

create table ticket_reservation (
    id varchar(36) not null,
    user_id varchar(36) not null,
    #first_name varchar(255),
    #last_name varchar(255),
    #email varchar(255),
    seating_id varchar(36) not null,
    seat_num int not null,
    #reservation_id varchar(36) not null,
    price float not null,
    expiry_time datetime not null,
    group_id varchar(36),
    group_accepted boolean,
    primary key (id),
    foreign key (user_id) references users(id),
    foreign key (group_id) references `user_groups` (id),
    foreign key (seating_id) references `seating_plan`(id)
    #foreign key (reservation_id) references event_reservation(id)
);

create table purchase_item (
    id varchar(36) not null,
    purchase_id varchar(36) not null,
    ticket_id varchar(36) not null,
    first_name varchar(255),
    last_name varchar(255),
    email varchar(255),

    payment_id varchar(255),

    primary key (id),
    foreign key (ticket_id) references ticket_reservation(id)
);


create table tickets (
    id          varchar(36) not null,
    user_id      varchar(36) not null,
    event_id     varchar(36) not null,
    section_id    varchar(36) not null,
    group_id     varchar(36),
    seat_no      int,
    first_name varchar(255),
    last_name varchar(255),
    email varchar(255),

    payment_id varchar(255),
    price long not null,

    primary key (id),
    foreign key (user_id) references users(id),
    foreign key (event_id) references `events`(id),
    foreign key (section_id) references seating_plan(id),
    foreign key (group_id) references `user_groups` (id)
);

create table categories (
    id          varchar(36) not null,
    event_id     varchar(36) not null,
    category    varchar(255) not null,
    primary key (id),
    foreign key (event_id) references `events`(id)
);

create table tags (
    id          varchar(36) not null,
    event_id     varchar(36) not null,
    tags        varchar(255) not null,
    primary key (id),
    foreign key (event_id) references `events`(id)
);

/*create table reviews (
     id          int not null,
     event_id    int not null,
     author_id   int not null,
     description text not null,
     primary key (id),
     foreign key (event_id) references events(id)
);

create table replies (
    id int not null,
    review_id int not null,
    author_id  int not null,
    comment_text text not null,
    primary key (id),
    foreign key (review_id) references reviews(id),
    foreign key  (author_id) references users(id)
);*/

create table event_comments (
    id varchar(36) not null,
    event_id varchar(36) not null,
    parent_id varchar(36),
    author_id varchar(36) not null,
    comment_title varchar(255),
    comment_text text not null,
    comment_time datetime not null,
    rating float,

    primary key (id),
    foreign key (event_id) references `events`(id),
    foreign key (parent_id) references event_comments(id),
    foreign key (author_id) references users(id)
);

create table reactions (
    id varchar(36) not null,
    comment_id varchar(36) not null,
    author_id varchar(36) not null,
    react_time datetime not null,
    react_type char(255) not null,

    primary key (id),
    foreign key (comment_id) references event_comments(id),
    foreign key (author_id) references users(id)
);

create table group_users (
    group_id varchar(36) not null,
    user_id varchar(36) not null,

    primary key (group_id, user_id),
    foreign key (group_id) references `user_groups`(id),
    foreign key (user_id) references users(id)
);

create table reset_tokens (
    id varchar(36) not null,
    user_id varchar(36) not null,
    expiry_time datetime not null,

    primary key (id),
    foreign key (user_id) references users(id)
);

create table invitation (
    id  varchar(36) not null,
    group_id  varchar(36) not null,
    reserve_id  varchar(36) not null,
    user_id  varchar(36) not null,
    
    primary key (id),
    foreign key (group_id) references `user_groups`(id),
    foreign key (reserve_id) references ticket_reservation(id),
    foreign key (user_id) references users(id)
);

create table document_term_count (
  term varchar(255) not null,
  term_count int not null,

  primary key (term)
);

create table tf_idf (
    term varchar(255) not null,
    event_id varchar(36) not null,

    term_freq double not null,
    document_count int not null,

    primary key (term, event_id),
    foreign key (term) references document_term_count(term),
    foreign key (event_id) references `events`(id)
);

create table user_interaction (
    id varchar(36) not null,
    interact_time datetime not null,
    user_id varchar(36) not null,
    event_id varchar(36) not null,
    interact_type int not null,
    rating double,

    primary key (id),
    foreign key (user_id) references `users`(id),
    foreign key (event_id) references `events`(id)
);

create table TestTable
(
    id    varchar(36),
    name  varchar(1024) null,
    email varchar(1024) null,
    constraint TestTable_pk
        primary key (id)
);


