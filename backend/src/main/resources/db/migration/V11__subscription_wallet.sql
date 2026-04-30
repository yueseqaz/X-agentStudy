alter table subscription_accounts
    add column wallet_balance_cents int not null default 0 after used_storage_mb;
