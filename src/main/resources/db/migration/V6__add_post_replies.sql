-- =========================================================================
-- 社内つぶやきボード V6: 投稿リプライ用の自己参照
-- Oracle XE 21c および H2(MODE=Oracle) の双方で動く DDL
-- =========================================================================

ALTER TABLE posts ADD parent_id NUMBER(19);

ALTER TABLE posts ADD CONSTRAINT posts_parent_fk
    FOREIGN KEY (parent_id) REFERENCES posts (id);

CREATE INDEX posts_parent_id_idx ON posts (parent_id);
