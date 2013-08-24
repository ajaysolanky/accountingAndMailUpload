

# --- !Ups

create table errors (
  id                        integer auto_increment not null,
  upload_id                 integer,
  upload_batch_attempt      integer,
  type                      varchar(255),
  subtype                   varchar(255),
  message                   varchar(255),
  updated_at                datetime,
  is_visible                tinyint(1) default 0,
  created_at                datetime not null,
  file_name                 varchar(255),
  constraint pk_errors primary key (id))
;

create table upload_session (
  id                        integer auto_increment not null,
  type                      varchar(255),
  constraint pk_upload_session primary key (id))
;

create table transaction (
  id                        integer auto_increment not null,
  upload_id                 integer,
  session_id                varchar(255),
  batch_id                  varchar(255),
  batch_post_day            varchar(255),
  transaction_processing_timestamp_gmt varchar(255),
  batch_completion          varchar(255),
  reporting_group           varchar(255),
  presenter                 varchar(255),
  merchant                  varchar(255),
  merchant_id               varchar(255),
  litle_payment_id          varchar(255),
  parent_litle_payment_id   varchar(255),
  merchant_order_number     varchar(255),
  customer_id               varchar(255),
  txn_type                  varchar(255),
  purchase_currency         varchar(255),
  purchase_amount           float,
  payment_type              varchar(255),
  bin                       varchar(255),
  account_suffix            integer,
  response_reason_code      varchar(255),
  response_reason_message   varchar(255),
  avs                       varchar(255),
  fraud_check_sum_response  varchar(255),
  payer_id                  varchar(255),
  merchant_transaction_id   varchar(255),
  affiliate                 varchar(255),
  campaign                  varchar(255),
  merchant_grouping_id      varchar(255),
  KEY IDX_LITLE_PAYMENT_ID (litle_payment_id),
  constraint pk_transaction primary key (id))
;

create table upload (
  id                        integer auto_increment not null,
  upload_batch_id           integer,
  file_name                 varchar(255) unique,
  upload_started            datetime,
  upload_finished           datetime,
  processing_started        datetime,
  processing_finished       datetime,
  record_count              integer,
  error_count               integer,
  skip_count                integer,
  should_delete             tinyint(1) default 0,
  type                      varchar(255),
  constraint pk_upload primary key (id))
;




# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table errors;

drop table upload_session;

drop table transaction;

drop table upload;

SET FOREIGN_KEY_CHECKS=1;

