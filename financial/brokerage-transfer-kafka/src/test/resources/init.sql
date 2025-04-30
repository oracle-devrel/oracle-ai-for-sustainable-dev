create table ms_account (
    customer_id varchar2(4000) primary key,
    balance     number not null
);

create table ms_overdraft (
    id          number(10) generated always as identity primary key,
    customer_id varchar2(4000),
    amount      number(10),
    constraint fk_customer_id foreign key (customer_id)
        references ms_account(customer_id)  on delete cascade
);
