from app.database import engine
from sqlalchemy import text
with engine.connect() as conn:
    r = conn.execute(text("SELECT column_name FROM information_schema.columns WHERE table_name='posts' AND column_name='marketplace_product_uid'"))
    row = r.fetchone()
    if row:
        print("EXISTS")
    else:
        print("NOT FOUND - adding now")
        conn.execute(text("ALTER TABLE posts ADD COLUMN marketplace_product_uid VARCHAR(36)"))
        conn.execute(text("CREATE INDEX ix_posts_marketplace_product_uid ON posts (marketplace_product_uid)"))
        conn.commit()
        print("ADDED")
