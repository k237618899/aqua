import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TmpDivaExport {
  static String esc(String s){ if(s==null) return ""; return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t"); }
  static String col(ResultSet rs, List<String> names) throws SQLException { ResultSetMetaData md=rs.getMetaData(); for(String n:names){ for(int i=1;i<=md.getColumnCount();i++){ if(md.getColumnLabel(i).equalsIgnoreCase(n)) return rs.getString(i);} } return null; }
  static Integer coli(ResultSet rs, List<String> names) throws SQLException { String v=col(rs,names); if(v==null||v.isBlank()) return null; try{return Integer.parseInt(v.trim());}catch(Exception e){return null;} }
  static int mapDiff(String raw,Integer num){ if(num!=null&&num>=0&&num<=3) return num; if(raw==null) return -1; String s=raw.trim().toUpperCase(Locale.ROOT); if(s.equals("EASY"))return 0; if(s.equals("NORMAL"))return 1; if(s.equals("HARD"))return 2; if(s.equals("EXTREME"))return 3; return -1; }
  static int mapEdition(String raw,Integer num){ if(num!=null&&(num==0||num==1)) return num; if(raw==null) return 0; String s=raw.trim().toUpperCase(Locale.ROOT); if(s.equals("ORIGINAL")) return 0; if(s.equals("EXTRA")) return 1; return 0; }
  static String tsOrDefault(String raw, LocalDateTime d){ if(raw==null||raw.isBlank()) return d.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); String r=raw.trim().replace(' ','T'); if(r.length()==10) r += "T00:00:00"; return r; }
  static boolean tableExists(Connection c,String t) throws SQLException { try(PreparedStatement ps=c.prepareStatement("select 1 from sqlite_master where type='table' and name=?")){ ps.setString(1,t); try(ResultSet rs=ps.executeQuery()){ return rs.next(); } } }

  public static void main(String[] args) throws Exception {
    String dbPath="data/db.sqlite"; String outPath="docs/dev/diva-song-batch-sample.json";
    LocalDateTime now=LocalDateTime.now().withNano(0), demoEndDef=now.plusDays(20), playableEndDef=LocalDateTime.of(2099,12,1,0,0,0);
    Class.forName("org.sqlite.JDBC");
    List<String> songs=new ArrayList<>(); int entryCount=0;
    try(Connection conn=DriverManager.getConnection("jdbc:sqlite:"+dbPath)){
      String songTable = tableExists(conn,"diva_pv") ? "diva_pv" : "diva_pv_info";
      String songSql = "SELECT p.* FROM "+songTable+" p WHERE EXISTS (SELECT 1 FROM diva_pv_entry e WHERE e.pv_id=p.pv_id) ORDER BY p.pv_id";
      try(Statement st=conn.createStatement(); ResultSet rs=st.executeQuery(songSql)){
        while(rs.next() && songs.size()<5){
          Integer pvId=coli(rs, Arrays.asList("pv_id","pvid","id")); if(pvId==null) continue;
          Integer bpm=coli(rs, Arrays.asList("bpm"));
          String songName=col(rs, Arrays.asList("song_name","name","title","pv_name"));
          String songNameEng=col(rs, Arrays.asList("song_name_eng","name_eng","title_eng","pv_name_en"));
          String songNameReading=col(rs, Arrays.asList("song_name_reading","name_reading","title_reading"));
          String arranger=col(rs, Arrays.asList("arranger"));
          String lyrics=col(rs, Arrays.asList("lyrics","lyricist"));
          String music=col(rs, Arrays.asList("music","composer"));
          Integer performer=coli(rs, Arrays.asList("performer_number","performer_num","performerNumber")); if(performer==null) performer=1;

          LinkedHashMap<Integer,String> uniq=new LinkedHashMap<>();
          try(PreparedStatement ps=conn.prepareStatement("SELECT * FROM diva_pv_entry WHERE pv_id=? ORDER BY rowid")){
            ps.setInt(1,pvId);
            try(ResultSet er=ps.executeQuery()){
              while(er.next()){
                int diff=mapDiff(col(er,Arrays.asList("difficulty","difficulty_name")), coli(er,Arrays.asList("difficulty","diff"))); if(diff<0||uniq.containsKey(diff)) continue;
                Integer version=coli(er,Arrays.asList("version")); if(version==null) version=1;
                int edition=mapEdition(col(er,Arrays.asList("edition","edition_name")), coli(er,Arrays.asList("edition")));
                String demoStart=tsOrDefault(col(er,Arrays.asList("demo_start","demoStart","disp_date")), now);
                String demoEnd=tsOrDefault(col(er,Arrays.asList("demo_end","demoEnd")), demoEndDef);
                String playableStart=tsOrDefault(col(er,Arrays.asList("playable_start","playableStart","start_date")), now);
                String playableEnd=tsOrDefault(col(er,Arrays.asList("playable_end","playableEnd","end_date")), playableEndDef);
                String entry="      {\n"+
                  "        \"difficulty\": "+diff+",\n"+
                  "        \"version\": "+version+",\n"+
                  "        \"edition\": "+edition+",\n"+
                  "        \"demoStart\": \""+esc(demoStart)+"\",\n"+
                  "        \"demoEnd\": \""+esc(demoEnd)+"\",\n"+
                  "        \"playableStart\": \""+esc(playableStart)+"\",\n"+
                  "        \"playableEnd\": \""+esc(playableEnd)+"\"\n"+
                  "      }";
                uniq.put(diff,entry);
              }
            }
          }
          if(uniq.isEmpty()) continue;
          StringBuilder s=new StringBuilder();
          s.append("  {\n");
          s.append("    \"pvId\": ").append(pvId).append(",\n");
          s.append("    \"bpm\": ").append(bpm==null?"null":bpm).append(",\n");
          s.append("    \"songName\": \"").append(esc(songName)).append("\",\n");
          s.append("    \"songNameEng\": \"").append(esc(songNameEng)).append("\",\n");
          s.append("    \"songNameReading\": \"").append(esc(songNameReading)).append("\",\n");
          s.append("    \"arranger\": \"").append(esc(arranger)).append("\",\n");
          s.append("    \"lyrics\": \"").append(esc(lyrics)).append("\",\n");
          s.append("    \"music\": \"").append(esc(music)).append("\",\n");
          s.append("    \"performerNumber\": ").append(performer).append(",\n");
          s.append("    \"entries\": [\n");
          int i=0,n=uniq.size(); for(String e:uniq.values()){ s.append(e); if(++i<n) s.append(","); s.append("\n"); }
          s.append("    ]\n");
          s.append("  }");
          songs.add(s.toString()); entryCount += uniq.size();
        }
      }
    }
    if(songs.size()<5) throw new RuntimeException("Only built "+songs.size()+" songs.");
    StringBuilder out=new StringBuilder("[\n"); for(int i=0;i<5;i++){ out.append(songs.get(i)); if(i<4) out.append(","); out.append("\n"); } out.append("]\n");
    Files.write(Paths.get(outPath), out.toString().getBytes(StandardCharsets.UTF_8));
    System.out.println("sqlite_jar="+args[0]);
    System.out.println("songs_emitted="+songs.size());
    System.out.println("entries_emitted="+entryCount);
    List<String> lines=Files.readAllLines(Paths.get(outPath), StandardCharsets.UTF_8); for(int i=0;i<Math.min(40,lines.size());i++) System.out.println(lines.get(i));
  }
}