"""Add marketplace_product_uid column to posts table."""
from app.database import engine
from sqlalchemy import text

with engine.connect() as conn:
    result = conn.execute(text(
        "SELECT column_name FROM information_schema.columns "
        "WHERE table_name='posts' AND column_name='marketplace_product_uid'"
    ))
    exists = result.fetchone()
    if not exists:
        conn.execute(text('ALTER TABLE posts ADD COLUMN marketplace_product_uid VARCHAR(36)'))
        conn.execute(text('CREATE INDEX ix_posts_marketplace_product_uid ON posts (marketplace_product_uid)'))
        conn.commit()
        print('Column marketplace_product_uid added successfully')
    else:
        print('Column already exists')
