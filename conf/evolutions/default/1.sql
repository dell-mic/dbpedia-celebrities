# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table `CELEBRITY` (`source` VARCHAR(254) NOT NULL PRIMARY KEY,`label` VARCHAR(254),`givenName` VARCHAR(254),`lastName` VARCHAR(254),`birthDate` VARCHAR(254),`birthPlaceLabel` VARCHAR(254),`abstractText` text,`thumbnail` VARCHAR(254),`children` VARCHAR(254),`height` VARCHAR(254),`residence` VARCHAR(254));

# --- !Downs

drop table `CELEBRITY`;

