package tw.repairtrack.app
import android.content.Context
import androidx.room.*
import org.json.*
import java.io.*
import java.util.zip.*
import java.util.UUID

@Entity data class Project(@PrimaryKey val id:String,val name:String)
@Entity(primaryKeys=["project","id"]) data class Defect(val project:String,val id:String,val payload:String)
@Entity data class Event(@PrimaryKey val id:String,val project:String,val defect:String,val status:String,val time:String,val note:String,val photos:String)
@Entity data class Photo(@PrimaryKey val id:String,val bytes:ByteArray)
@Dao interface Store {
 @Query("SELECT * FROM Project ORDER BY name") fun projects():List<Project>
 @Query("SELECT * FROM Defect WHERE project=:p") fun defects(p:String):List<Defect>
 @Query("SELECT * FROM Defect") fun allDefects():List<Defect>
 @Query("SELECT * FROM Event WHERE project=:p AND defect=:d ORDER BY time") fun history(p:String,d:String):List<Event>
 @Query("SELECT * FROM Event") fun events():List<Event>
 @Query("SELECT * FROM Photo") fun photos():List<Photo>
 @Query("SELECT * FROM Photo WHERE id=:id") fun photo(id:String):Photo?
 @Insert(onConflict=OnConflictStrategy.REPLACE) fun put(p:Project)
 @Insert(onConflict=OnConflictStrategy.REPLACE) fun put(d:Defect)
 @Insert fun put(e:Event)
 @Insert fun put(p:Photo)
 @Query("DELETE FROM Photo") fun clearPhotos()
 @Query("DELETE FROM Event") fun clearEvents()
 @Query("DELETE FROM Defect") fun clearDefects()
 @Query("DELETE FROM Project") fun clearProjects()
}
@Database(entities=[Project::class,Defect::class,Event::class,Photo::class],version=1,exportSchema=false)
abstract class LocalDb:RoomDatabase(){abstract fun store():Store; companion object {fun open(c:Context)=Room.databaseBuilder(c,LocalDb::class.java,"repairtrack-v3.db").build()}}
fun decode(s:String):Map<String,String>{val j=JSONObject(s);return j.keys().asSequence().associateWith{j.getString(it)}}
fun encode(m:Map<String,String>)=JSONObject(m).toString()
fun Event.paths():List<String>{val a=JSONArray(photos);return (0 until a.length()).map{a.getString(it)}}
object Backup {
 fun write(db:LocalDb,out:OutputStream){db.runInTransaction{val s=db.store();ZipOutputStream(out).use{z->fun entry(n:String,b:ByteArray){z.putNextEntry(ZipEntry(n));z.write(b);z.closeEntry()};val j=JSONObject();j.put("version",1);j.put("projects",JSONArray(s.projects().map{JSONObject().put("id",it.id).put("name",it.name)}));j.put("defects",JSONArray(s.allDefects().map{JSONObject().put("project",it.project).put("id",it.id).put("payload",it.payload)}));j.put("events",JSONArray(s.events().map{JSONObject().put("id",it.id).put("project",it.project).put("defect",it.defect).put("status",it.status).put("time",it.time).put("note",it.note).put("photos",it.photos)}));entry("manifest.json",j.toString().toByteArray());s.photos().forEach{entry("photos/${it.id}",it.bytes)}}}}
 fun restore(db:LocalDb,input:InputStream){val files=mutableMapOf<String,ByteArray>();var total=0L;ZipInputStream(input).use{z->while(true){val e=z.nextEntry?:break;require(!files.containsKey(e.name));val b=ByteArrayOutputStream();val buf=ByteArray(8192);while(true){val n=z.read(buf);if(n<0)break;total+=n;require(total<=150_000_000){"備份超過150MB"};b.write(buf,0,n)};files[e.name]=b.toByteArray()}};val j=JSONObject(requireNotNull(files.remove("manifest.json")).toString(Charsets.UTF_8));require(j.getInt("version")==1);fun rows(k:String):List<JSONObject>{val a=j.getJSONArray(k);return (0 until a.length()).map{a.getJSONObject(it)}};val ps=rows("projects").map{Project(it.getString("id"),it.getString("name"))};val ds=rows("defects").map{Defect(it.getString("project"),it.getString("id"),it.getString("payload"))};val es=rows("events").map{Event(it.getString("id"),it.getString("project"),it.getString("defect"),it.getString("status"),it.getString("time"),it.getString("note"),it.getString("photos"))};require(ps.map{it.id}.distinct().size==ps.size);require(ds.all{d->ps.any{it.id==d.project}&&decode(d.payload)["id"]==d.id});require(es.all{e->ds.any{it.project==e.project&&it.id==e.defect}&&e.paths().all{files.containsKey("photos/$it")}});require(files.keys.all{it.startsWith("photos/")&&it.removePrefix("photos/").matches(Regex("[a-f0-9-]{36}"))});db.runInTransaction{val s=db.store();s.clearPhotos();s.clearEvents();s.clearDefects();s.clearProjects();ps.forEach{s.put(it)};ds.forEach{s.put(it)};es.forEach{s.put(it)};files.forEach{(n,b)->s.put(Photo(n.removePrefix("photos/"),b))}}}
}
