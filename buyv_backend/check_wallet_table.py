from dotenv import load_dotenv; load_dotenv()
from sqlalchemy import create_engine, text
import os

engine = create_engine(os.environ['DATABASE_URL'])
with engine.connect() as conn:
    rows = conn.execute(text(
        "SELECT column_name, data_type, column_default, is_nullable "
        "FROM information_schema.columns "
        "WHERE table_name = 'promoter_wallets' "
        "ORDER BY ordinal_position"
    )).fetchall()
    print("=== promoter_wallets columns ===")
    for r in rows:
        print(r)
