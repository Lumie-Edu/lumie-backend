-- Preserve tenant history when users, students, or staff are hard-deleted.
--
-- This migration is forward-only. It relaxes historical ownership references
-- from ON DELETE CASCADE to ON DELETE SET NULL so operational records survive
-- identity deletion. The exam_results.student_deleted marker preserves the
-- existing "unregistered student" meaning of student_id IS NULL. Application
-- services block deletion while current or future operations remain open.

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists assignment_submissions
    alter column student_id drop not null;

alter table if exists attendance_records
    alter column student_id drop not null;

alter table if exists class_enrollments
    alter column student_id drop not null;

alter table if exists counseling_reservations
    alter column student_id drop not null,
    alter column user_id drop not null;

alter table if exists counseling_schedules
    alter column user_id drop not null;

alter table if exists sms_messages
    alter column sender_id drop not null;

alter table if exists exam_results
    add column if not exists student_deleted boolean not null default false;

alter table if exists classes
    add column if not exists teacher_deleted boolean not null default false;

-- teacher_deleted is prospective. Classes whose teacher_id was already nulled
-- before this migration remain indistinguishable from intentionally unassigned
-- classes because no reliable historical teacher-delete signal exists.

alter table if exists assignment_submissions
    drop constraint if exists assignment_submissions_student_id_fkey;

alter table if exists assignment_submissions
    add constraint assignment_submissions_student_id_fkey
    foreign key (student_id) references students(id) on delete set null;

alter table if exists attendance_records
    drop constraint if exists attendance_records_student_id_fkey;

alter table if exists attendance_records
    add constraint attendance_records_student_id_fkey
    foreign key (student_id) references students(id) on delete set null;

alter table if exists class_enrollments
    drop constraint if exists class_enrollments_student_id_fkey;

alter table if exists class_enrollments
    add constraint class_enrollments_student_id_fkey
    foreign key (student_id) references students(id) on delete set null;

alter table if exists counseling_reservations
    drop constraint if exists counseling_reservations_user_id_fkey;

alter table if exists counseling_reservations
    add constraint counseling_reservations_user_id_fkey
    foreign key (user_id) references users(id) on delete set null;

alter table if exists counseling_schedules
    drop constraint if exists counseling_schedules_user_id_fkey;

alter table if exists counseling_schedules
    add constraint counseling_schedules_user_id_fkey
    foreign key (user_id) references users(id) on delete set null;

alter table if exists exam_results
    drop constraint if exists exam_results_student_id_fkey;

alter table if exists exam_results
    add constraint exam_results_student_id_fkey
    foreign key (student_id) references students(id) on delete set null;

alter table if exists sms_messages
    drop constraint if exists sms_messages_sender_id_fkey;

alter table if exists sms_messages
    add constraint sms_messages_sender_id_fkey
    foreign key (sender_id) references staff(id) on delete set null;

create or replace function mark_exam_results_student_deleted()
returns trigger
language plpgsql
as $$
begin
    update exam_results
       set student_deleted = true,
           phone_number = null
     where student_id = old.id;

    update counseling_reservations
       set student_id = null
     where student_id = old.id;

    return old;
end;
$$;

drop trigger if exists trg_mark_exam_results_student_deleted on students;

create trigger trg_mark_exam_results_student_deleted
before delete on students
for each row
execute function mark_exam_results_student_deleted();

create or replace function mark_classes_teacher_deleted()
returns trigger
language plpgsql
as $$
begin
    update classes
       set teacher_deleted = true
     where teacher_id = old.id;
    return old;
end;
$$;

drop trigger if exists trg_mark_classes_teacher_deleted on staff;

create trigger trg_mark_classes_teacher_deleted
before delete on staff
for each row
execute function mark_classes_teacher_deleted();
