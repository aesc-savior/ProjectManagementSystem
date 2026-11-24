package pm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 会议：创建、查询、回执、取消、冲突检测
public class MeetingDao {

    private static final String BASE_SELECT =
            "select m.id, m.topic, m.project_id, m.start_time, m.end_time, m.location, m.agenda,"
                    + " m.organizer_id, m.organizer, m.status, m.cancel_reason, m.cancel_time,"
                    + " m.attach_name, m.attach_path, m.attach_size, m.create_time, m.update_time,"
                    + " ifnull(p.project_no,'') as project_no, ifnull(p.name,'') as project_name,"
                    + " (select count(*) from pm_meeting_attendee a where a.meeting_id=m.id) as attendee_count,"
                    + " (select count(*) from pm_meeting_attendee a where a.meeting_id=m.id"
                    + "   and a.reply_status='ACCEPTED') as accepted_count,"
                    + " (select count(*) from pm_meeting_attendee a where a.meeting_id=m.id"
                    + "   and a.reply_status='DECLINED') as declined_count,"
                    + " ifnull((select a.reply_status from pm_meeting_attendee a"
                    + "   where a.meeting_id=m.id and a.user_id=%UID%),'') as my_reply,"
                    + " ifnull((select a.reply_reason from pm_meeting_attendee a"
                    + "   where a.meeting_id=m.id and a.user_id=%UID%),'') as my_reply_reason"
                    + " from pm_meeting m left join pm_project p on p.id=m.project_id";

    private static String s(String v) {
        return v == null ? "" : v;
    }

    private static String cut(String v, int max) {
        return v.length() > max ? v.substring(0, max) : v;
    }

    public static String scope(String uid, boolean all) {
        if (all) {
            return "";
        }
        if (!Db.isId(uid)) {
            return " and 1=0";
        }
        return " and (m.organizer_id=" + uid
                + " or exists(select 1 from pm_meeting_attendee a2"
                + " where a2.meeting_id=m.id and a2.user_id=" + uid + "))";
    }

    public static String buildWhere(String projectId, String userId, String from, String to,
                                    String status, String keyword, String uid, boolean all) {
        List<String> w = new ArrayList<String>();
        w.add("m.del_flag=0");
        if (Db.isId(s(projectId))) {
            w.add("m.project_id=" + projectId);
        }
        if (Db.isId(s(userId))) {
            w.add("exists(select 1 from pm_meeting_attendee a"
                    + " where a.meeting_id=m.id and a.user_id=" + userId + ")");
        }
        if (from != null && from.matches("\\d{4}-\\d{2}-\\d{2}")) {
            w.add("m.start_time>='" + from + " 00:00:00'");
        }
        if (to != null && to.matches("\\d{4}-\\d{2}-\\d{2}")) {
            w.add("m.start_time<date_add('" + to + "', interval 1 day)");
        }
        if (status != null && status.matches("[A-Z]{3,10}")) {
            w.add("m.status='" + status + "'");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = Db.esc(keyword);
            w.add("(m.topic like '%" + k + "%' or m.location like '%" + k + "%'"
                    + " or m.agenda like '%" + k + "%')");
        }
        return " where " + String.join(" and ", w) + scope(uid, all);
    }

    public static long count(String projectId, String userId, String from, String to,
                             String status, String keyword, String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_meeting m"
                + buildWhere(projectId, userId, from, to, status, keyword, uid, all));
    }

    public static List<Map<String, String>> list(String projectId, String userId, String from,
                                                 String to, String status, String keyword, int page, int size,
                                                 String uid, boolean all) {
        int offset = Math.max(0, (page - 1) * size);
        return Db.query(BASE_SELECT.replace("%UID%", Db.isId(uid) ? uid : "0")
                + buildWhere(projectId, userId, from, to, status, keyword, uid, all)
                + " order by m.start_time desc, m.id desc limit " + offset + "," + size);
    }

    public static List<Map<String, String>> listAll(String projectId, String userId, String from,
                                                    String to, String status, String keyword, String uid, boolean all) {
        return Db.query(BASE_SELECT.replace("%UID%", Db.isId(uid) ? uid : "0")
                + buildWhere(projectId, userId, from, to, status, keyword, uid, all)
                + " order by m.start_time desc, m.id desc");
    }

    public static Map<String, String> get(String id, String uid) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT.replace("%UID%", Db.isId(uid) ? uid : "0")
                + " where m.id=" + id + " and m.del_flag=0");
    }

    public static String save(String id, Map<String, String> p) {
        String topic = Db.esc(cut(s(p.get("topic")), 200));
        String projectId = Db.isId(s(p.get("projectId"))) ? s(p.get("projectId")) : "0";
        String start = Db.esc(s(p.get("startTime")));
        String end = Db.esc(s(p.get("endTime")));
        String location = Db.esc(cut(s(p.get("location")), 200));
        String agenda = Db.esc(cut(s(p.get("agenda")), 20000));
        String organizerId = Db.isId(s(p.get("organizerId"))) ? s(p.get("organizerId")) : "null";
        String organizer = Db.esc(cut(s(p.get("organizer")), 60));
        String creator = Db.esc(cut(s(p.get("creator")), 60));

        if (Db.isId(id)) {
            Db.update("update pm_meeting set topic='" + topic + "', project_id=" + projectId
                    + ", start_time='" + start + "', end_time='" + end + "', location='" + location
                    + "', agenda='" + agenda + "', update_time='" + Db.now()
                    + "' where id=" + id);
            return id;
        }

        long newId = Db.insert("insert into pm_meeting(topic,project_id,start_time,end_time,"
                + "location,agenda,organizer_id,organizer,status,create_time,update_time,creator)"
                + " values('" + topic + "'," + projectId + ",'" + start + "','" + end + "','"
                + location + "','" + agenda + "'," + organizerId + ",'" + organizer
                + "','PUBLISHED','" + Db.now() + "','" + Db.now() + "','" + creator + "')");
        return newId > 0 ? String.valueOf(newId) : "";
    }

    public static List<Map<String, String>> attendees(String meetingId) {
        if (!Db.isId(meetingId)) {
            return new ArrayList<Map<String, String>>();
        }
        return Db.query("select a.id, a.user_id, a.user_name, a.reply_status,"
                + " a.reply_reason, a.reply_time, u.dept_name, u.role_code"
                + " from pm_meeting_attendee a left join pm_user u on u.id=a.user_id"
                + " where a.meeting_id=" + meetingId + " order by a.id");
    }

    public static List<String> attendeeIds(String meetingId) {
        List<String> ids = new ArrayList<String>();
        for (Map<String, String> r : attendees(meetingId)) {
            ids.add(r.get("user_id"));
        }
        return ids;
    }

    public static void addAttendee(String meetingId, String userId, String status) {
        if (!Db.isId(meetingId) || !Db.isId(userId)) {
            return;
        }
        String st = "ACCEPTED".equals(status) ? "ACCEPTED" : "PENDING";
        Db.update("insert ignore into pm_meeting_attendee(meeting_id,user_id,user_name,reply_status)"
                + " select " + meetingId + ", id, user_name, '" + st + "' from pm_user"
                + " where id=" + userId + " and status='NORMAL' and del_flag=0");
    }

    public static void removeAttendee(String meetingId, String userId) {
        if (Db.isId(meetingId) && Db.isId(userId)) {
            Db.update("delete from pm_meeting_attendee where meeting_id=" + meetingId
                    + " and user_id=" + userId);
        }
    }

    public static void ensureOrganizer(String meetingId, String userId) {
        if (Db.isId(meetingId) && Db.isId(userId)) {
            Db.update("update pm_meeting_attendee set reply_status='ACCEPTED'"
                    + " where meeting_id=" + meetingId + " and user_id=" + userId);
        }
    }

    public static void resetReplies(String meetingId, String organizerId) {
        if (!Db.isId(meetingId)) {
            return;
        }
        String sql = "update pm_meeting_attendee set reply_status='PENDING',"
                + " reply_reason=null, reply_time=null where meeting_id=" + meetingId;
        if (Db.isId(organizerId)) {
            sql += " and user_id<>" + organizerId;
        }
        Db.update(sql);
    }

    public static boolean isAttendee(String meetingId, String userId) {
        if (!Db.isId(meetingId) || !Db.isId(userId)) {
            return false;
        }
        return Db.queryLong("select count(*) from pm_meeting_attendee where meeting_id="
                + meetingId + " and user_id=" + userId) > 0;
    }

    public static boolean reply(String meetingId, String userId, boolean accept, String reason) {
        if (!Db.isId(meetingId) || !Db.isId(userId)) {
            return false;
        }
        int n = Db.update("update pm_meeting_attendee set reply_status='"
                + (accept ? "ACCEPTED" : "DECLINED") + "', reply_reason='"
                + Db.esc(cut(s(reason), 200)) + "', reply_time='" + Db.now()
                + "' where meeting_id=" + meetingId + " and user_id=" + userId);
        return n > 0;
    }

    public static boolean cancel(String meetingId, String reason) {
        if (!Db.isId(meetingId)) {
            return false;
        }
        int n = Db.update("update pm_meeting set status='CANCELED', cancel_reason='"
                + Db.esc(cut(s(reason), 200)) + "', cancel_time='" + Db.now()
                + "', update_time='" + Db.now() + "' where id=" + meetingId
                + " and status='PUBLISHED'");
        return n > 0;
    }

    public static List<Map<String, String>> conflicts(String excludeId, String start, String end,
                                                      List<String> userIds) {
        List<Map<String, String>> empty = new ArrayList<Map<String, String>>();
        if (start == null || end == null || userIds == null || userIds.isEmpty()
                || !start.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
                || !end.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return empty;
        }
        List<String> ids = new ArrayList<String>();
        for (String u : userIds) {
            if (Db.isId(s(u))) {
                ids.add(u);
            }
        }
        if (ids.isEmpty()) {
            return empty;
        }
        return Db.query("select m.id, m.topic, m.start_time, m.end_time, m.location, u.user_name"
                + " from pm_meeting m"
                + " join pm_meeting_attendee a on a.meeting_id=m.id"
                + " join pm_user u on u.id=a.user_id"
                + " where m.del_flag=0 and m.status='PUBLISHED'"
                + (Db.isId(excludeId) ? " and m.id<>" + excludeId : "")
                + " and a.user_id in (" + String.join(",", ids) + ")"
                + " and m.start_time < '" + end + "' and m.end_time > '" + start + "'"
                + " order by m.start_time, u.user_name");
    }

    public static void updateAttach(String meetingId, String name, String path, long size) {
        if (!Db.isId(meetingId)) {
            return;
        }
        Db.update("update pm_meeting set attach_name='" + Db.esc(cut(s(name), 200))
                + "', attach_path='" + Db.esc(path) + "', attach_size=" + size
                + ", update_time='" + Db.now() + "' where id=" + meetingId);
    }

    public static void clearAttach(String meetingId) {
        if (Db.isId(meetingId)) {
            Db.update("update pm_meeting set attach_name=null, attach_path=null, attach_size=0,"
                    + " update_time='" + Db.now() + "' where id=" + meetingId);
        }
    }

    public static String statusText(Map<String, String> r) {
        if (r == null) {
            return "";
        }
        if ("CANCELED".equals(r.get("status"))) {
            return "已取消";
        }
        String end = r.get("end_time");
        if (end != null && !end.isEmpty() && end.compareTo(Db.now()) < 0) {
            return "已结束";
        }
        return "已发布";
    }

    public static String inputTime(String v) {
        if (v == null || v.length() < 16) {
            return "";
        }
        return v.substring(0, 10) + "T" + v.substring(11, 16);
    }
}