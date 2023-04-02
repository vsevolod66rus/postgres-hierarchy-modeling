-- adjacency list hierarchy, filled with scala

create table adjacency_list_hierarchy
(
    id       integer unique,
    position varchar,
    name     varchar,
    parent   integer references adjacency_list_hierarchy (id)
);

create index on adjacency_list_hierarchy (id);
create index on adjacency_list_hierarchy (parent);


-- adjacency list hierarchy read hierarchy
WITH RECURSIVE subordinates AS (SELECT *
                                FROM adjacency_list_hierarchy
                                WHERE id = 1
                                UNION
                                SELECT t.*
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent)
SELECT *
FROM subordinates
order by id;


-- adjacency list hierarchy read hierarchy with count
WITH RECURSIVE subordinates AS (SELECT *
                                FROM adjacency_list_hierarchy
                                WHERE id = 1
                                UNION
                                SELECT t.*
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent)
SELECT *,
       (WITH RECURSIVE subordinates AS (SELECT id
                                        FROM adjacency_list_hierarchy
                                        WHERE id = subordinates.id
                                        UNION
                                        SELECT t.id
                                        FROM adjacency_list_hierarchy t
                                                 INNER JOIN subordinates s ON s.id = t.parent)
        SELECT count(*)
        FROM subordinates) as cnt
from subordinates;


-- nested set
create table nested_set_hierarchy
(
    id         integer unique,
    position   varchar,
    name       varchar,
    parent     integer references nested_set_hierarchy (id),
    level      integer,
    lt         integer,
    rt         integer,
    node_count integer
);

create index on nested_set_hierarchy (parent);


--nested set from adjacency list hierarchy, about 55 min on Ryzen 7
insert into nested_set_hierarchy (id, position, name, parent, level, lt, rt, node_count)
select id,
       position,
       name,
       parent,
       height,
       traversal_id * 2 - height                            as lt,
       (node_cnt - 1) * 2 + (traversal_id * 2 - height) + 1 as rt,
       node_cnt
from (WITH RECURSIVE subordinates AS (SELECT *, 1 as height
                                      FROM adjacency_list_hierarchy
                                      WHERE id = 1
                                      UNION
                                      SELECT t.*, height + 1
                                      FROM adjacency_list_hierarchy t
                                               INNER JOIN subordinates s ON s.id = t.parent)
      SELECT row_number() over () as traversal_id,
             *,
             (WITH RECURSIVE subordinates AS (SELECT id
                                              FROM adjacency_list_hierarchy
                                              WHERE id = subordinates.id
                                              UNION
                                              SELECT t.id
                                              FROM adjacency_list_hierarchy t
                                                       INNER JOIN subordinates s ON s.id = t.parent)
              SELECT count(*)
              FROM subordinates)  as node_cnt
      FROM subordinates) adj_list;


--adj list with some fields https://www.sqlservercentral.com/articles/hierarchies-on-steroids-1-convert-an-adjacency-list-to-nested-sets
create table adjacency_list_hierarchy_busted
(
    id        integer,
    position  varchar,
    name      varchar,
    parent    integer,
    level     integer,
    sort_path varchar
);
create index on adjacency_list_hierarchy_busted (id);
create index on adjacency_list_hierarchy_busted (parent);
create index on adjacency_list_hierarchy_busted (sort_path);


insert into adjacency_list_hierarchy_busted
WITH RECURSIVE subordinates AS (SELECT *, 1 as level, lpad(id::varchar, 7, '0') as sort_path
                                FROM adjacency_list_hierarchy
                                WHERE id = 1
                                UNION
                                SELECT t.*, level + 1, sort_path || lpad(t.id::varchar, 7, '0')
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent)
SELECT *
from subordinates;


-- one more nested set table for competition
create table nested_set_hierarchy_from_busted
(
    id         integer unique,
    position   varchar,
    name       varchar,
    parent     integer,
    level      integer,
    lt         integer,
    rt         integer,
    node_count integer
);


-- for quick adj list to nested set
-- we have 10 levels
create table levels
(
    n integer
);
insert into levels
SELECT num * 7 + 1
FROM generate_series(0, 9) num;


-- filled in ~50 sec
insert into nested_set_hierarchy_from_busted (id, position, name, parent, level, lt, rt, node_count)
select id,
       position,
       name,
       parent,
       level,
       traversal_id * 2 - level                            as lt,
       (node_cnt - 1) * 2 + (traversal_id * 2 - level) + 1 as rt,
       node_cnt
from (WITH RECURSIVE subordinates AS (SELECT *
                                      FROM adjacency_list_hierarchy_busted
                                      WHERE id = 1
                                      UNION
                                      SELECT t.*
                                      FROM adjacency_list_hierarchy_busted t
                                               INNER JOIN subordinates s ON s.id = t.parent)
      SELECT row_number() over () as traversal_id,
             *
      FROM subordinates) adj_list
         join (select SUBSTRING(h.sort_path, l.n, 7)::int as eid, count(*) as node_cnt
               from adjacency_list_hierarchy_busted h,
                    levels l
               where 1 <= l.n
                 and l.n <= length(sort_path)
               group by SUBSTRING(h.sort_path, l.n, 7)) coint_table on coint_table.eid = adj_list.id;


-- ltree
create extension ltree;

create table ltree_hierarchy
(
    id       integer unique,
    position varchar,
    name     varchar,
    path     ltree
);


CREATE INDEX path_gist_idx ON ltree_hierarchy USING GIST (path);
CREATE INDEX path_gist_idx ON ltree_hierarchy USING GIST (path);


insert into ltree_hierarchy
WITH RECURSIVE subordinates AS (SELECT *, 1 as level, name as sort_path
                                FROM adjacency_list_hierarchy
                                WHERE id = 2
                                UNION
                                SELECT t.*, level + 1, sort_path || '.' || t.name
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent)
SELECT id, position, name, sort_path::ltree
from subordinates;


-- ltree examples
SELECT *
FROM ltree_hierarchy
WHERE path ~ '*.defenseMinister.*';

-- colonelGeneral_1 -> colonelGeneral_228
update ltree_hierarchy
set path = 'defenseMinister.colonelGeneral_228':: ltree || subpath(path, nlevel('defenseMinister.colonelGeneral_1') - 1)
from ltree_hierarchy
where path <@ 'defenseMinister.colonelGeneral_1'
  and path != 'defenseMinister.colonelGeneral_1';


-- or
update ltree_hierarchy
set path = subpath('defenseMinister.colonelGeneral_228', 0, -1) || 'colonelGeneral_1':: ltree ||
           subpath(path, nlevel('defenseMinister.colonelGeneral_228'))
where path ~ 'defenseMinister.colonelGeneral_228.*{1,}';
-- 109,087 rows affected in 1 s 888 ms


select *, (select count(*) from ltree_hierarchy where ltree_hierarchy.path <@ t.path)
from (select *
      from ltree_hierarchy
      where path <@ 'defenseMinister') as t
order by id;

select *
from ltree_hierarchy
where path <@ 'defenseMinister.colonelGeneral_228';


-- for scala mapping, requires rolesuper
DO
$func$
    BEGIN
        CASE
            WHEN (select rolsuper
                  from pg_roles
                  where rolname = current_user)
                then create
                    or replace function ltree_invarchar(varchar) returns ltree as
                $$
                select ltree_in($1::cstring);
                $$
                    language sql immutable;
            ELSE null;
            END CASE;
    END
$func$;


DO
$func$
    BEGIN
        CASE
            WHEN (select rolsuper
                  from pg_roles
                  where rolname = current_user)
                then do
                $$
                    begin
                        create cast (varchar as ltree) with function ltree_invarchar(varchar) as implicit;
                    exception
                        when duplicate_object then null;
                    end
                $$;
            ELSE null;
            END CASE;
    END
$func$;


-- or just
create
    or replace function ltree_invarchar(varchar) returns ltree as
$$
select ltree_in($1::cstring);
$$
    language sql immutable;

do
$$
    begin
        create cast (varchar as ltree) with function ltree_invarchar(varchar) as implicit;
    exception
        when duplicate_object then null;
    end
$$;







