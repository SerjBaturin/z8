package org.zenframework.z8.server.types;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.zenframework.z8.server.base.file.Folders;
import org.zenframework.z8.server.base.file.InputOnlyFileItem;
import org.zenframework.z8.server.base.table.system.Files;
import org.zenframework.z8.server.engine.RmiIO;
import org.zenframework.z8.server.engine.RmiSerializable;
import org.zenframework.z8.server.exceptions.ThreadInterruptedException;
import org.zenframework.z8.server.json.Json;
import org.zenframework.z8.server.json.JsonWriter;
import org.zenframework.z8.server.json.parser.JsonArray;
import org.zenframework.z8.server.json.parser.JsonObject;
import org.zenframework.z8.server.runtime.RCollection;
import org.zenframework.z8.server.runtime.RLinkedHashMap;
import org.zenframework.z8.server.utils.IOUtils;
import org.zenframework.z8.server.utils.NumericUtils;

public class file extends primary implements RmiSerializable, Serializable {

	private static final long serialVersionUID = -2542688680678439014L;
	static public final String EOL = "\r\n";

	static public final String separator = "/";

	public guid id = guid.Null;
	public string name = new string();
	public string path = new string();
	public date time = new date();
	public integer size = integer.Zero;
	public guid user = guid.Null;
	public string author = new string();

	public RLinkedHashMap<string, string> details = new RLinkedHashMap<string, string>();

	private FileItem value;

	private long offset = 0;
	private int partLength = 0;

	public JsonObject json;

	public static FileItem createFileItem(string name) {
		return createFileItem(name.get());
	}

	public static FileItem createFileItem(String name) {
		return new DiskFileItem(null, null, false, name, NumericUtils.Megabyte, null);
	}

	public file() {
		super();
	}

	public file(guid id) {
		this(id, null, null, null);
	}

	public file(File file) {
		this(null, file.getName(), null, file.getPath(), file.length(), new date(file.lastModified()));
	}

	public file(FileItem file) {
		this(file, null, null);
	}

	public file(FileItem value, String instanceId, String path) {
		this(null, value.getName(), instanceId, path, value.getSize(), null);
		this.value = value;
	}

	public file(guid id, String name, String instanceId, String path) {
		this(id, name, instanceId, path, 0, null);
	}

	public file(guid id, String name, String instanceId, String path, long size, date time) {
		super();
		this.id = new guid(id);
		this.path = new string(getRelativePath(path));
		this.name = new string(name);
		this.size = new integer(size);
		this.time = time != null ? time : new date();
	}

	public file(file file) {
		super();
		set(file);
	}

	public file(String path) {
		this(new string(path));
	}

	public file(string path) {
		operatorAssign(path);
	}

	public file(guid id, String path) {
		this(id, new string(path));
	}

	public file(guid id, string path) {
		this.id = id;
		operatorAssign(path);
	}

	protected file(JsonObject json) {
		super();
		set(json);
	}

	public void set(file file) {
		this.id = file.id;
		this.name = file.name;
		this.path = file.path;
		this.time = file.time;
		this.size = file.size;
		this.user = file.user;
		this.author = file.author;
		this.value = file.value;
		this.details = file.details;
		this.json = file.json;
	}

	public FileItem get() {
		return value;
	}

	public void set(FileItem value) {
		this.value = value;
	}

	public void set(File value) {
		this.value = new InputOnlyFileItem(value, value.getName());
	}

	protected void set(JsonObject json) {
		path = new string(json.getString(json.has(Json.file) ? Json.file : Json.path));
		name = new string(json.has(Json.name) ? json.getString(Json.name) : "");
		size = new integer(json.has(Json.size) ? json.getString(Json.size) : "");
		id = new guid(json.has(Json.id) ? json.getString(Json.id) : "");
		user = new guid(json.has(Json.user) ? json.getString(Json.user) : "");
		author = new string(json.has(Json.author) ? json.getString(Json.author) : "");

		String jsonTime = json.has(Json.time) ? json.getString(Json.time) : "";
		try {
			time = jsonTime == null || jsonTime.indexOf('/') == -1 ? new date(jsonTime) : new date(jsonTime, "D/M/y H:m:s");
		} catch(NumberFormatException e) {
			time = new date();
		}

		this.json = json;
	}

	static public Collection<file> parse(String json) {
		List<file> result = new ArrayList<file>();

		JsonArray array = new JsonArray(json);

		for(int i = 0; i < array.length(); i++)
			result.add(new file(array.getJsonObject(i)));

		return result;
	}

	static public String toJson(Collection<file> files) {
		JsonArray array = new JsonArray();

		for(file file : files)
			array.add(file.toJsonObject());

		return array.toString();
	}

	public long offset() {
		return offset;
	}

	public int partLength() {
		return partLength;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public JsonObject toJsonObject() {
		json = new JsonObject();
		json.put(Json.name, name);
		json.put(Json.time, time);
		json.put(Json.size, size);
		json.put(Json.path, path);
		json.put(Json.id, id);
		json.put(Json.user, user);
		json.put(Json.author, author);
		json.put(Json.details, details);
		return json;
	}

	public void write(JsonWriter writer) {
		writer.writeProperty(Json.name, name);
		writer.writeProperty(Json.time, time);
		writer.writeProperty(Json.size, size);
		writer.writeProperty(Json.path, path);
		writer.writeProperty(Json.id, id);
		writer.writeProperty(Json.user, user);
		writer.writeProperty(Json.author, author);

		writer.startObject(Json.details);
		for(string key : details.keySet())
			writer.writeProperty(key, details.get(key));
		writer.finishObject();
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof file && id != null && id.equals(((file)object).id);
	}

	@Override
	public int compareTo(primary primary) {
		return 0;
	}

	@Override
	public String toString() {
		return toJsonObject().toString();
	}

	public String baseName() {
		return FilenameUtils.getBaseName(name.get());
	}

	public String extension() {
		return FilenameUtils.getExtension(name.get());
	}

	public File toFile() {
		return getAbsolutePath(path.get());
	}

	public InputStream getInputStream() {
		try {
			return value == null ? null : value.getInputStream();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public OutputStream getOutputStream() {
		try {
			return value == null ? null : value.getOutputStream();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		serialize(out);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		deserialize(in);
	}

	@Override
	public void serialize(ObjectOutputStream out) throws IOException {
		RmiIO.writeLong(out, serialVersionUID);

		RmiIO.writeGuid(out, id);
		RmiIO.writeString(out, name);
		RmiIO.writeString(out, path);
		RmiIO.writeDate(out, time);
		RmiIO.writeInteger(out, size);
		RmiIO.writeGuid(out, user);
		RmiIO.writeString(out, author);

		RmiIO.writeLong(out, offset);
		RmiIO.writeInt(out, partLength);
		RmiIO.writeBoolean(out, value != null);

		if(value != null) {
			InputStream in = value.getInputStream();

			long size = in.available();
			RmiIO.writeLong(out, size);

			try {
				IOUtils.copyLarge(in, out, size, false);
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
	}

	@Override
	public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
		@SuppressWarnings("unused")
		long version = RmiIO.readLong(in);

		id = RmiIO.readGuid(in);
		name = new string(RmiIO.readString(in));
		path = new string(RmiIO.readString(in));
		time = RmiIO.readDate(in);
		size = RmiIO.readInteger(in);
		user = RmiIO.readGuid(in);
		author = new string(RmiIO.readString(in));

		offset = RmiIO.readLong(in);
		partLength = RmiIO.readInt(in);

		if(RmiIO.readBoolean(in)) {
			long size = RmiIO.readLong(in);

			value = createFileItem(name);
			OutputStream out = value.getOutputStream();

			try {
				IOUtils.copyLarge(in, out, size, false);
			} finally {
				IOUtils.closeQuietly(out);
			}
		}
	}

	static public file createTempFile(String extension) {
		return createTempFile(new File(Folders.Base, Folders.Temp), extension);
	}

	static public file createLogFile(String folder, String extension) {
		return createTempFile(FileUtils.getFile(Folders.Base, Folders.Logs, folder), extension);
	}

	static public file createTempFile(File folder, String extension) {
		folder.mkdirs();

		if(extension != null && !extension.isEmpty())
			extension = extension.charAt(0) != '.' ? "." + extension : extension;
		else
			extension = "";

		String name = new date().format("Y-MM-dd HH-mm-ss") + extension;
		return new file(new File(folder, name));
	}

	public String read() {
		return read(encoding.Default);
	}

	public String read(encoding encoding) {
		InputStream input = null;

		try {
			input = getInputStream();
			if(input == null) {
				File file = getAbsolutePath(path.get());
				input = new FileInputStream(file);
			}
			return IOUtils.readText(input, Charset.forName(encoding.toString()));
		} catch(IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(input);
		}
	}

	public void write(String content) {
		write(content, true);
	}

	public void write(String content, boolean append) {
		write(content, encoding.Default, true);
	}

	public void write(String content, encoding charset, boolean append) {
		try {
			write(content.getBytes(charset.toString()), append);
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public void write(byte[] bytes, boolean append) {
		try {
			if(path.isEmpty()) {
				set(createTempFile("txt"));
				path = new string(getRelativePath(path.get()));
			}

			File file = getAbsolutePath(path.get());

			OutputStream output = new FileOutputStream(file, append);
			output.write(bytes);
			IOUtils.closeQuietly(output);
			size = new integer(file.length());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public File getAbsolutePath() {
		return getAbsolutePath(path.get());
	}

	static public File getAbsolutePath(String path) {
		return getAbsolutePath(new File(path));
	}

	static public File getAbsolutePath(File file) {
		if(file.isAbsolute())
			return file;

		file = new File(Folders.Base, file.getPath());
		return file;
	}

	static public String getRelativePath(File file) {
		return getRelativePath(file.getPath());
	}

	static public String getRelativePath(String path) {
		if(path != null && path.startsWith(Folders.Base.getPath()))
			path = path.substring(Folders.Base.getPath().length() + 1);

		return path;
	}

	public file nextPart() throws IOException {
		if(Thread.interrupted())
			throw new ThreadInterruptedException();

		Files.get(this);

		offset += partLength;

		if(offset == size.get())
			return null;

		byte[] bytes = new byte[512 * NumericUtils.Kilobyte];
		partLength = readPart(offset, bytes);

		InputStream input = new ByteArrayInputStream(bytes, 0, partLength);

		set(createFileItem(name));

		IOUtils.zip(input, getOutputStream());

		return this;
	}

	private int readPart(long offset, byte[] bytes) throws IOException {
		InputStream input = null;
		try {
			input = getInputStream();
			input.skip(offset);
			return IOUtils.read(input, bytes);
		} finally {
			IOUtils.closeQuietly(input);
		}
	}

	public boolean addPartTo(File target) throws IOException {
		RandomAccessFile output = new RandomAccessFile(target.getPath(), "rw");
		output.seek(offset);

		IOUtils.unzip(getInputStream(), output);
		return target.length() == size.get();
	}

	public void operatorAssign(string path) {
		File file = new File(path.get());

		this.path = path;
		this.name = new string(file.getName());
	}

	public bool z8_exists() {
		return new bool(toFile().exists());
	}

	public bool z8_isDirectory() {
		File file = getAbsolutePath(path.get());
		return new bool(file.isDirectory());
	}

	public string z8_baseName() {
		return new string(baseName());
	}

	public string z8_extension() {
		return new string(extension());
	}

	public RCollection<file> z8_listFiles() {
		File[] files = getAbsolutePath(path.get()).listFiles();

		RCollection<file> result = new RCollection<file>();

		if(files == null)
			return result;

		for(File file : files)
			result.add(new file(file));

		return result;
	}

	public string z8_read() {
		return new string(read());
	}

	public string z8_read(encoding encoding) {
		return new string(read(encoding));
	}

	public void z8_write(string content) {
		write(content.get(), true);
	}

	public void z8_write(string content, bool append) {
		write(content.get(), encoding.Default, true);
	}

	public void z8_write(string content, encoding encoding, bool append) {
		write(content.get(), encoding, append.get());
	}

	static public RCollection<file> z8_parse(string json) {
		return new RCollection<file>(parse(json.get()));
	}

	static public string z8_toJson(RCollection<file> classes) {
		return new string(toJson(classes));
	}

	static public string z8_name(string name) {
		return new string(FilenameUtils.getName(name.get()));
	}

	static public string z8_baseName(string name) {
		return new string(FilenameUtils.getBaseName(name.get()));
	}

	static public string z8_extension(string name) {
		return new string(FilenameUtils.getExtension(name.get()));
	}
}
