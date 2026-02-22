"""
One-time migration: ensure promoter_wallets.id is auto-incrementing in PostgreSQL.
Run once: python fix_wallet_id_sequence.py
"""
from dotenv import load_dotenv
load_dotenv()

import os
from sqlalchemy import create_engine, text

engine = create_engine(os.environ["DATABASE_URL"])

with engine.begin() as conn:  # begin() auto-commits on success
    # Check current column default
    row = conn.execute(text("""
        SELECT column_default
        FROM information_schema.columns
        WHERE table_name = 'promoter_wallets' AND column_name = 'id'
    """)).fetchone()

    current_default = row[0] if row else None
    print(f"Current id column_default: {current_default}")

    if current_default and "nextval" in str(current_default):
        print("Column already uses a sequence. No action needed.")
    else:
        print("Fixing id column to use auto-increment sequence...")

        # Get current max id to seed the sequence correctly
        max_id_row = conn.execute(text("SELECT COALESCE(MAX(id), 0) FROM promoter_wallets")).fetchone()
        max_id = max_id_row[0] if max_id_row else 0
        print(f"Current max id: {max_id}")

        # Step 1: Create sequence starting from max_id + 1
        conn.execute(text(f"""
            CREATE SEQUENCE IF NOT EXISTS promoter_wallets_id_seq
            START WITH {max_id + 1}
            INCREMENT BY 1
            NO MINVALUE NO MAXVALUE CACHE 1;
        """))
        print("Sequence created.")

        # Step 2: Set the sequence ownership
        conn.execute(text("""
            ALTER SEQUENCE promoter_wallets_id_seq
            OWNED BY promoter_wallets.id;
        """))

        # Step 3: Set the column default to use the sequence
        conn.execute(text("""
            ALTER TABLE promoter_wallets
            ALTER COLUMN id SET DEFAULT nextval('promoter_wallets_id_seq');
        """))
        print("Column default set to nextval.")

    print("Done! promoter_wallets.id is now auto-incrementing.")
