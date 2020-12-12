create table client (
	id uuid default random_uuid(),
	name varchar(100) not null,
    PRIMARY KEY (id)
);

insert into client (name) values ('testClient');


create table organization (
    code varchar(3) primary key
);

insert into organization values ('SBR'), ('ALF'), ('TKF');


create table currency (
    code varchar(3) primary key
);

insert into currency values ('RUB'), ('USD'), ('EUR');


create table account (
	id uuid default random_uuid(),
	client_id uuid,
	org_code varchar(3),
	currency varchar(3),
	balance numeric,
    PRIMARY KEY (id),
	FOREIGN KEY (client_id) REFERENCES client (id),
	FOREIGN KEY (org_code) REFERENCES organization (code),
	FOREIGN KEY (currency) REFERENCES currency (code)
);

create table tran (
	id uuid default random_uuid(),
	account_id uuid,
	amount numeric,
	currency varchar(3),
	date timestamp with time zone default now(),
	PRIMARY KEY (id),
	FOREIGN KEY (account_id) REFERENCES account (id),
	FOREIGN KEY (currency) REFERENCES currency (code)
);
