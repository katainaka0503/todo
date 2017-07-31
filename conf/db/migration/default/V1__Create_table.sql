create table todos(
    id serial primary key,
    title varchar(30) not null,
    description text not null
);