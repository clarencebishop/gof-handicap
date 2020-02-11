CREATE TABLE scores (
  id serial PRIMARY KEY,
  golfer_name VARCHAR(32) NOT NULL,
  course_name VARCHAR(16) NOT NULL,
  date_played VARCHAR(16) NOT NULL,
  rating VARCHAR(8),
  slope VARCHAR(8),
  score VARCHAR(4) NOT NULL
);
