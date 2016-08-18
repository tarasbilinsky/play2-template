# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table test1 (
  id                            bigint auto_increment not null,
  color                         integer,
  a                             double,
  b                             float,
  c                             float,
  money                         double,
  money2                        double,
  constraint ck_test1_color check (color in (0,1,2)),
  constraint pk_test1 primary key (id)
);

create table user (
  id                            bigint auto_increment not null,
  name                          varchar(255),
  password                      varchar(255),
  active                        tinyint(1) default 0,
  x                             integer,
  xy                            integer,
  primary_user_role_id          bigint,
  constraint ck_user_x check (x in (0,1,2,3,4)),
  constraint ck_user_xy check (xy in (0,1,2,3,4)),
  constraint pk_user primary key (id)
);

create table user_user_role (
  user_id                       bigint not null,
  user_role_id                  bigint not null,
  constraint pk_user_user_role primary key (user_id,user_role_id)
);

create table user_user_permission (
  user_id                       bigint not null,
  user_permission_id            bigint not null,
  constraint pk_user_user_permission primary key (user_id,user_permission_id)
);

create table user_permission (
  id                            bigint auto_increment not null,
  title                         varchar(255),
  sys_name                      varchar(255),
  order_number                  integer,
  active                        tinyint(1) default 0,
  constraint pk_user_permission primary key (id)
);

create table user_role (
  id                            bigint auto_increment not null,
  title                         varchar(255),
  sys_name                      varchar(255),
  order_number                  integer,
  active                        tinyint(1) default 0,
  constraint pk_user_role primary key (id)
);

create table user_session (
  id                            bigint auto_increment not null,
  start                         bigint,
  last                          bigint,
  closed                        tinyint(1) default 0,
  user_id                       bigint,
  constraint pk_user_session primary key (id)
);

alter table user add constraint fk_user_primary_user_role_id foreign key (primary_user_role_id) references user_role (id) on delete restrict on update restrict;
create index ix_user_primary_user_role_id on user (primary_user_role_id);

alter table user_user_role add constraint fk_user_user_role_user foreign key (user_id) references user (id) on delete restrict on update restrict;
create index ix_user_user_role_user on user_user_role (user_id);

alter table user_user_role add constraint fk_user_user_role_user_role foreign key (user_role_id) references user_role (id) on delete restrict on update restrict;
create index ix_user_user_role_user_role on user_user_role (user_role_id);

alter table user_user_permission add constraint fk_user_user_permission_user foreign key (user_id) references user (id) on delete restrict on update restrict;
create index ix_user_user_permission_user on user_user_permission (user_id);

alter table user_user_permission add constraint fk_user_user_permission_user_permission foreign key (user_permission_id) references user_permission (id) on delete restrict on update restrict;
create index ix_user_user_permission_user_permission on user_user_permission (user_permission_id);

alter table user_session add constraint fk_user_session_user_id foreign key (user_id) references user (id) on delete restrict on update restrict;
create index ix_user_session_user_id on user_session (user_id);


# --- !Downs

alter table user drop foreign key fk_user_primary_user_role_id;
drop index ix_user_primary_user_role_id on user;

alter table user_user_role drop foreign key fk_user_user_role_user;
drop index ix_user_user_role_user on user_user_role;

alter table user_user_role drop foreign key fk_user_user_role_user_role;
drop index ix_user_user_role_user_role on user_user_role;

alter table user_user_permission drop foreign key fk_user_user_permission_user;
drop index ix_user_user_permission_user on user_user_permission;

alter table user_user_permission drop foreign key fk_user_user_permission_user_permission;
drop index ix_user_user_permission_user_permission on user_user_permission;

alter table user_session drop foreign key fk_user_session_user_id;
drop index ix_user_session_user_id on user_session;

drop table if exists test1;

drop table if exists user;

drop table if exists user_user_role;

drop table if exists user_user_permission;

drop table if exists user_permission;

drop table if exists user_role;

drop table if exists user_session;

