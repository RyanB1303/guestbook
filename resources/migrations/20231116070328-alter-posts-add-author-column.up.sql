ALTER TABLE posts
ADD COLUMN author TEXT
REFERENCES users(login)
ON DELETE SET NULL
oN UPDATE CASCADE;