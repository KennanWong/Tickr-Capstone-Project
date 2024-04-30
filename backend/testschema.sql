-- TestTable, put in the schema TestSchema
create table TestTable
(
    id    int auto_increment,
    name  varchar(1024) null,
    email varchar(1024) null,
    constraint TestTable_pk
        primary key (id)
);

