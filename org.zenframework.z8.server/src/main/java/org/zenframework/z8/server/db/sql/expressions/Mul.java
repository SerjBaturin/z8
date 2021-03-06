package org.zenframework.z8.server.db.sql.expressions;

import org.zenframework.z8.server.db.DatabaseVendor;
import org.zenframework.z8.server.db.sql.FormatOptions;
import org.zenframework.z8.server.db.sql.SqlToken;
import org.zenframework.z8.server.db.sql.functions.numeric.Mod;

public class Mul extends Operator {
	public Mul(SqlToken l, Operation oper, SqlToken r) {
		super(l, oper, r);
	}

	@Override
	public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
		switch(operation) {
		case Mul: {
			return left.format(vendor, options) + "*" + right.format(vendor, options);
		}

		case Div: {
			return left.format(vendor, options) + "/" + right.format(vendor, options);
		}

		case Mod: {
			return new Mod(left, right).format(vendor, options);
		}

		default:
			throw new UnsupportedOperationException();
		}
	}
}
