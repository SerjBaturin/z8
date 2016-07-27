package org.zenframework.z8.server.ie;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.zenframework.z8.server.base.file.Folders;
import org.zenframework.z8.server.base.file.InputOnlyFileItem;
import org.zenframework.z8.server.base.table.system.Files;
import org.zenframework.z8.server.types.file;

public class Import {
	static public boolean importObject(Object object) {
		if(object instanceof Message) {
			return Import.importMessage((Message)object);
		} else if(object instanceof file) {
			return Import.importFile((file)object);
		}
		throw new RuntimeException("Unsupported object type '" + (object != null ? object.getClass().getCanonicalName() : "null"));
	}

	static public boolean importFile(file file) {
		File target = FileUtils.getFile(Folders.Base, Folders.Temp, file.path.get());

		long offset = file.offset();

		if(offset == 0) {
			target.getParentFile().mkdirs();
			target.delete();
		} else if(!target.exists() || (offset + file.partLength()) < target.length())
			return false;

		try {
			if(!file.addPartTo(target))
				return true;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}

		try {
			Files files = Files.newInstance();
			file.set(new InputOnlyFileItem(target, file.name.get()));

			if(!files.hasRecord(file.id))
				files.add(file);
			else
				files.updateFile(file);

			return true;
		} finally {
			target.delete();
		}
	}

	static public boolean importMessage(Message message) {
		message.importData();
		return true;
	}
}
