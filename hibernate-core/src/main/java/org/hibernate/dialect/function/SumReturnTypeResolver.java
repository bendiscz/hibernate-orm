/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.List;
import java.util.function.Supplier;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.extractArgumentType;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.extractArgumentValuedMapping;

/**
 * Resolve according to JPA spec 4.8.5
 *
 * {@code SUM} returns:
 * <ul>
 * <li>{@code Long} when applied to state fields of integral types (other than {@code BigInteger});
 * <li>{@code Double} when applied to state fields of floating point types;
 * <li>{@code BigInteger} when applied to state fields of type {@code BigInteger};
 * <li>and {@code BigDecimal} when applied to state fields of type {@code BigDecimal}.
 * </ul>
 *
 * @author Christian Beikov
 */
class SumReturnTypeResolver implements FunctionReturnTypeResolver {

	private final BasicType<Long> longType;
	private final BasicType<Double> doubleType;
	private final BasicType<BigInteger> bigIntegerType;
	private final BasicType<BigDecimal> bigDecimalType;

	public SumReturnTypeResolver(TypeConfiguration typeConfiguration) {
		final BasicType<Long> longType = typeConfiguration.getBasicTypeForJavaType(Long.class);
		final BasicType<Double> doubleType = typeConfiguration.getBasicTypeForJavaType(Double.class);
		final BasicType<BigInteger> bigIntegerType = typeConfiguration.getBasicTypeForJavaType(BigInteger.class);
		final BasicType<BigDecimal> bigDecimalType = typeConfiguration.getBasicTypeForJavaType(BigDecimal.class);
		this.longType = longType;
		this.doubleType = doubleType;
		this.bigIntegerType = bigIntegerType;
		this.bigDecimalType = bigDecimalType;
	}

	@Override
	public ReturnableType<?> resolveFunctionReturnType(
			ReturnableType<?> impliedType,
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		if (impliedType != null) {
			return impliedType;
		}
		final ReturnableType<?> argType = extractArgumentType( arguments, 1 );
		final BasicType<?> basicType;
		if (argType instanceof BasicType<?>) {
			basicType = (BasicType<?>) argType;
		}
		else {
			basicType = typeConfiguration.getBasicTypeForJavaType( argType.getJavaType() );
			if (basicType == null) {
				return impliedType;
			}
		}
		switch ( basicType.getJdbcType().getDefaultSqlTypeCode() ) {
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.INTEGER:
			case Types.BIGINT:
				return longType;
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
				return doubleType;
			case Types.DECIMAL:
			case Types.NUMERIC:
				return BigInteger.class.isAssignableFrom( basicType.getJavaType() ) ? bigIntegerType : bigDecimalType;
		}
		return bigDecimalType;
	}

	@Override
	public BasicValuedMapping resolveFunctionReturnType(
			Supplier<BasicValuedMapping> impliedTypeAccess,
			List<? extends SqlAstNode> arguments) {
		if (impliedTypeAccess != null) {
			final BasicValuedMapping basicValuedMapping = impliedTypeAccess.get();
			if (basicValuedMapping != null) {
				return basicValuedMapping;
			}
		}
		// Resolve according to JPA spec 4.8.5
		final BasicValuedMapping specifiedArgType = extractArgumentValuedMapping( arguments, 1 );
		switch ( specifiedArgType.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.INTEGER:
			case Types.BIGINT:
				return longType;
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
				return doubleType;
			case Types.DECIMAL:
			case Types.NUMERIC:
				final Class<?> argTypeClass = specifiedArgType.getJdbcMapping()
						.getJavaTypeDescriptor()
						.getJavaTypeClass();
				return BigInteger.class.isAssignableFrom(argTypeClass) ? bigIntegerType : bigDecimalType;
		}
		return bigDecimalType;
	}

}
