# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table permission (
  id                            bigint auto_increment not null,
  title                         varchar(255),
  sys_name                      varchar(255),
  order_number                  integer,
  active                        tinyint(1) default 0,
  constraint pk_permission primary key (id)
);

create table user (
  id                            bigint auto_increment not null,
  name                          varchar(255),
  password                      varchar(255),
  constraint pk_user primary key (id)
);

create table user_user_role (
  user_id                       bigint not null,
  user_role_id                  bigint not null,
  constraint pk_user_user_role primary key (user_id,user_role_id)
);

create table user_permission (
  user_id                       bigint not null,
  permission_id                 bigint not null,
  constraint pk_user_permission primary key (user_id,permission_id)
);

create table user_role (
  id                            bigint auto_increment not null,
  title                         varchar(255),
  sys_name                      varchar(255),
  order_number                  integer,
  active                        tinyint(1) default 0,
  constraint pk_user_role primary key (id)
);

alter table user_user_role add constraint fk_user_user_role_user foreign key (user_id) references user (id) on delete restrict on update restrict;
create index ix_user_user_role_user on user_user_role (user_id);

alter table user_user_role add constraint fk_user_user_role_user_role foreign key (user_role_id) references user_role (id) on delete restrict on update restrict;
create index ix_user_user_role_user_role on user_user_role (user_role_id);

alter table user_permission add constraint fk_user_permission_user foreign key (user_id) references user (id) on delete restrict on update restrict;
create index ix_user_permission_user on user_permission (user_id);

alter table user_permission add constraint fk_user_permission_permission foreign key (permission_id) references permission (id) on delete restrict on update restrict;
create index ix_user_permission_permission on user_permission (permission_id);


# --- !Downs

alter table user_user_role drop foreign key fk_user_user_role_user;
drop index ix_user_user_role_user on user_user_role;

alter table user_user_role drop foreign key fk_user_user_role_user_role;
drop index ix_user_user_role_user_role on user_user_role;

alter table user_permission drop foreign key fk_user_permission_user;
drop index ix_user_permission_user on user_permission;

alter table user_permission drop foreign key fk_user_permission_permission;
drop index ix_user_permission_permission on user_permission;

drop table if exists permission;

drop table if exists user;

drop table if exists user_user_role;

drop table if exists user_permission;

drop table if exists user_role;

