"""
Fix promoter_wallets: migrate id column from UUID to SERIAL (integer autoincrement).
Safe: preserves existing rows, reassigns integer IDs.
Run: python fix_wallet_id_uuid_to_int.py
"""
from dotenv import load_dotenv
load_dotenv()

import os
from sqlalchemy import create_engine, text

engine = create_engine(os.environ["DATABASE_URL"])

with engine.begin() as conn:
    # Check current type
    row = conn.execute(text("""
        SELECT column_name, data_type, column_default
        FROM information_schema.columns
        WHERE table_name = 'promoter_wallets' AND column_name = 'id'
    """)).fetchone()
    print(f"Current id column: {row}")

    col_type = row[1] if row else None

    if col_type in ('integer', 'bigint') and row[2] and 'nextval' in str(row[2]):
        print("Already integer with sequence. Nothing to do.")
    else:
        print(f"Migrating id from '{col_type}' to SERIAL integer...")

        # Count existing rows
        count = conn.execute(text("SELECT COUNT(*) FROM promoter_wallets")).scalar()
        print(f"Existing rows: {count}")

        if count == 0:
            # Safe to drop and recreate
            print("No data — dropping and recreating table with correct schema...")
            conn.execute(text("DROP TABLE IF EXISTS promoter_wallets CASCADE"))
            conn.execute(text("""
                CREATE TABLE promoter_wallets (
                    id SERIAL PRIMARY KEY,
                    user_id VARCHAR(36) UNIQUE NOT NULL,
                    total_earned FLOAT DEFAULT 0.0,
                    pending_amount FLOAT DEFAULT 0.0,
                    available_amount FLOAT DEFAULT 0.0,
                    withdrawn_amount FLOAT DEFAULT 0.0,
                    total_sales_count INTEGER DEFAULT 0,
                    promoter_level VARCHAR(50),
                    bank_name VARCHAR(100),
                    bank_account_number VARCHAR(100),
                    bank_account_holder VARCHAR(100),
                    bank_swift_code VARCHAR(50),
                    created_at TIMESTAMP DEFAULT NOW(),
                    updated_at TIMESTAMP DEFAULT NOW()
                )
            """))
            print("Table recreated with SERIAL id.")
        else:
            # Has data — migrate carefully
            print(f"Has {count} rows — migrating with temp column...")

            # 1. Add temp integer column
            conn.execute(text("ALTER TABLE promoter_wallets ADD COLUMN id_new SERIAL"))

            # 2. Drop old PK constraint
            conn.execute(text("ALTER TABLE promoter_wallets DROP CONSTRAINT IF EXISTS promoter_wallets_pkey"))

            # 3. Drop old id column
            conn.execute(text("ALTER TABLE promoter_wallets DROP COLUMN id"))

            # 4. Rename new column to id
            conn.execute(text("ALTER TABLE promoter_wallets RENAME COLUMN id_new TO id"))

            # 5. Add PK constraint
            conn.execute(text("ALTER TABLE promoter_wallets ADD PRIMARY KEY (id)"))

            print(f"Migration done. Rows preserved: {count}")

    # Verify
    verify = conn.execute(text("""
        SELECT column_name, data_type, column_default
        FROM information_schema.columns
        WHERE table_name = 'promoter_wallets' AND column_name = 'id'
    """)).fetchone()
    print(f"Final id column: {verify}")
    print("Done!")
