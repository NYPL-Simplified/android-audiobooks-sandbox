package com.example.android.uamp.model;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;


/**
 * DONE:
 * - Made sure the current sample manifest does read into the model, except for attype and spine.properties, which is minor.
 *
 * TODO:
 * - write javadocs
 * - fix attype -- looks like just naming it type would work, see schemaIsBasedOn, it works there.
 * - what happens if not all or more elements are present in the manifest?  make sure still reads ok if fields are missing (half day).
 * add all the possible other fields and attributes tha might possibly have anywhere (few days).
 * - create date type properties to copy string type dates into
 * - see of sample_rate can be an integer (read old convos with Hadrien about it)
 * - make constants to use, like for the MIME types and the schema types.
 * - put in convenience methods to convert duration around between seconds, minutes, hours, and fancy strings.
 *
 *
 * Created by daryachernikhova on 5/4/17.
 */

public class ManifestModel {

	@SerializedName("metadata")
	@Expose
	private Metadata metadata;
	@SerializedName("links")
	@Expose
	private List<Link> links;
	@SerializedName("spine")
	@Expose
	private List<Spine> spine;
	@SerializedName("timeline")
	@Expose
	private List<Timeline> timeline;


	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

	public List<Spine> getSpine() {
		return spine;
	}

	public void setSpine(List<Spine> spine) {
		this.spine = spine;
	}

	public List<Timeline> getTimeline() {
		return timeline;
	}

	public void setTimeline(List<Timeline> timeline) {
		this.timeline = timeline;
	}


	// --------------------------- UTILITY METHODS ---------------------------
	public String sanitizeString(String dirtyData) {
		if (dirtyData == null) {
			return "";
		}
		return dirtyData;
	}


	// --------------------------- INNER CLASSES ---------------------------
	public class Author {
		@SerializedName("name")
		@Expose
		private Name name;
		@SerializedName("sort_as")
		@Expose
		private String sortAs;

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		public String getSortAs() {
			return sortAs;
		}

		public void setSortAs(String sortAs) {
			this.sortAs = sortAs;
		}

	}



	public class Link {
		@SerializedName("rel")
		@Expose
		private String rel;
		@SerializedName("href")
		@Expose
		private String href;
		@SerializedName("type")
		@Expose
		private String type;
		@SerializedName("height")
		@Expose
		private Integer height;
		@SerializedName("width")
		@Expose
		private Integer width;

		public String getRel() {
			return rel;
		}

		public void setRel(String rel) {
			this.rel = rel;
		}

		public String getHref() {
			return href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Integer getHeight() {
			return height;
		}

		public void setHeight(Integer height) {
			this.height = height;
		}

		public Integer getWidth() {
			return width;
		}

		public void setWidth(Integer width) {
			this.width = width;
		}

	}



	public class Metadata {
		@SerializedName("attype")
		@Expose
		private String attype;
		@SerializedName("identifier")
		@Expose
		private String identifier;
		@SerializedName("title")
		@Expose
		private String title;
		@SerializedName("author")
		@Expose
		private List<Author> author = null;
		@SerializedName("narrator")
		@Expose
		private List<Narrator> narrator = null;
		@SerializedName("language")
		@Expose
		private String language;
		@SerializedName("description")
		@Expose
		private String description;
		@SerializedName("subject")
		@Expose
		private List<String> subject = null;
		@SerializedName("schema:typicalAgeRange")
		@Expose
		private String schemaTypicalAgeRange;
		@SerializedName("publisher")
		@Expose
		private String publisher;
		@SerializedName("published")
		@Expose
		private String published;
		@SerializedName("modified")
		@Expose
		private String modified;
		@SerializedName("duration")
		@Expose
		private Integer duration;
		@SerializedName("type")
		@Expose
		private String type;
		@SerializedName("bitrate")
		@Expose
		private Integer bitrate;
		@SerializedName("samplerate")
		@Expose
		private String samplerate;
		@SerializedName("channels")
		@Expose
		private Integer channels;
		@SerializedName("schema:hasPart")
		@Expose
		private List<SchemaHasPart> schemaHasPart = null;
		@SerializedName("schema:license")
		@Expose
		private String schemaLicense;
		@SerializedName("schema:isBasedOn")
		@Expose
		private SchemaIsBasedOn schemaIsBasedOn;

		public String getAttype() {
			return attype;
		}

		public void setAttype(String attype) {
			this.attype = attype;
		}

		public String getIdentifier() {
			return identifier;
		}

		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<Author> getAuthor() {
			return author;
		}

		public void setAuthor(List<Author> author) {
			this.author = author;
		}

		public List<Narrator> getNarrator() {
			return narrator;
		}

		public void setNarrator(List<Narrator> narrator) {
			this.narrator = narrator;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public List<String> getSubject() {
			return subject;
		}

		public void setSubject(List<String> subject) {
			this.subject = subject;
		}

		public String getSchemaTypicalAgeRange() {
			return schemaTypicalAgeRange;
		}

		public void setSchemaTypicalAgeRange(String schemaTypicalAgeRange) {
			this.schemaTypicalAgeRange = schemaTypicalAgeRange;
		}

		public String getPublisher() {
			return publisher;
		}

		public void setPublisher(String publisher) {
			this.publisher = publisher;
		}

		public String getPublished() {
			return published;
		}

		public void setPublished(String published) {
			this.published = published;
		}

		public String getModified() {
			return modified;
		}

		public void setModified(String modified) {
			this.modified = modified;
		}

		public Integer getDuration() {
			return duration;
		}

		public void setDuration(Integer duration) {
			this.duration = duration;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Integer getBitrate() {
			return bitrate;
		}

		public void setBitrate(Integer bitrate) {
			this.bitrate = bitrate;
		}

		public String getSamplerate() {
			return samplerate;
		}

		public void setSamplerate(String samplerate) {
			this.samplerate = samplerate;
		}

		public Integer getChannels() {
			return channels;
		}

		public void setChannels(Integer channels) {
			this.channels = channels;
		}

		public List<SchemaHasPart> getSchemaHasPart() {
			return schemaHasPart;
		}

		public void setSchemaHasPart(List<SchemaHasPart> schemaHasPart) {
			this.schemaHasPart = schemaHasPart;
		}

		public String getSchemaLicense() {
			return schemaLicense;
		}

		public void setSchemaLicense(String schemaLicense) {
			this.schemaLicense = schemaLicense;
		}

		public SchemaIsBasedOn getSchemaIsBasedOn() {
			return schemaIsBasedOn;
		}

		public void setSchemaIsBasedOn(SchemaIsBasedOn schemaIsBasedOn) {
			this.schemaIsBasedOn = schemaIsBasedOn;
		}

	}



	public class Name {
		@SerializedName("ru")
		@Expose
		private String ru;
		@SerializedName("en")
		@Expose
		private String en;

		public String getRu() {
			return ru;
		}

		public void setRu(String ru) {
			this.ru = ru;
		}

		public String getEn() {
			return en;
		}

		public void setEn(String en) {
			this.en = en;
		}

	}


	public class Narrator {

		@SerializedName("name")
		@Expose
		private String name;
		@SerializedName("image")
		@Expose
		private String image;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getImage() {
			return image;
		}

		public void setImage(String image) {
			this.image = image;
		}

	}



	public class SchemaHasPart {

		@SerializedName("@type")
		@Expose
		private String type;
		@SerializedName("schema:url")
		@Expose
		private String schemaUrl;
		@SerializedName("schema:license")
		@Expose
		private String schemaLicense;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getSchemaUrl() {
			return schemaUrl;
		}

		public void setSchemaUrl(String schemaUrl) {
			this.schemaUrl = schemaUrl;
		}

		public String getSchemaLicense() {
			return schemaLicense;
		}

		public void setSchemaLicense(String schemaLicense) {
			this.schemaLicense = schemaLicense;
		}

	}



	public class SchemaIsBasedOn {

		@SerializedName("@type")
		@Expose
		private String type;
		@SerializedName("title")
		@Expose
		private String title;
		@SerializedName("schema:license")
		@Expose
		private String schemaLicense;
		@SerializedName("schema:url")
		@Expose
		private String schemaUrl;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getSchemaLicense() {
			return schemaLicense;
		}

		public void setSchemaLicense(String schemaLicense) {
			this.schemaLicense = schemaLicense;
		}

		public String getSchemaUrl() {
			return schemaUrl;
		}

		public void setSchemaUrl(String schemaUrl) {
			this.schemaUrl = schemaUrl;
		}

	}



	public class Spine {

		@SerializedName("href")
		@Expose
		private String href;
		@SerializedName("type")
		@Expose
		private String type;
		@SerializedName("bitrate")
		@Expose
		private Integer bitrate;
		@SerializedName("samplerate")
		@Expose
		private String samplerate;
		@SerializedName("channels")
		@Expose
		private Integer channels;
		@SerializedName("duration")
		@Expose
		private Integer duration;
		@SerializedName("title")
		@Expose
		private String title;

		//@SerializedName("properties")
		//@Expose
		//private Properties_ properties;

		public String getHref() {
			return href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Integer getBitrate() {
			return bitrate;
		}

		public void setBitrate(Integer bitrate) {
			this.bitrate = bitrate;
		}

		public String getSamplerate() {
			return samplerate;
		}

		public void setSamplerate(String samplerate) {
			this.samplerate = samplerate;
		}

		public Integer getChannels() {
			return channels;
		}

		public void setChannels(Integer channels) {
			this.channels = channels;
		}

		public Integer getDuration() {
			return duration;
		}

		public void setDuration(Integer duration) {
			this.duration = duration;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

//		public Properties_ getProperties() {
//			return properties;
//		}
//
//		public void setProperties(Properties_ properties) {
//			this.properties = properties;
//		}

	}


	public class Timeline {

		@SerializedName("href")
		@Expose
		private String href;
		@SerializedName("title")
		@Expose
		private String title;

		public String getHref() {
			return href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

	}

}// ManifestModel













