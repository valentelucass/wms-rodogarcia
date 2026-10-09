//#region node_modules/lossless-json/lib/esm/config.js
/**
* Get and/or set configuration options
* @deprecated There is no config anymore
*/
function config(_options) {
	throw new Error("config is deprecated, support for circularRefs is removed from the library. If you encounter circular references in your data structures, please rethink your datastructures: better prevent circular references in the first place.");
}
//#endregion
//#region node_modules/lossless-json/lib/esm/utils.js
/**
* Test whether a string contains an integer number
*/
function isInteger(value) {
	return INTEGER_REGEX.test(value);
}
var INTEGER_REGEX = /^-?[0-9]+$/;
/**
* Test whether a string contains a number
* http://stackoverflow.com/questions/13340717/json-numbers-regular-expression
*/
function isNumber(value) {
	return NUMBER_REGEX.test(value);
}
var NUMBER_REGEX = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/;
/**
* Test whether a string can be safely represented with a number
* without information loss.
*
* When approx is true, floating point numbers that lose a few digits but
* are still approximately equal in value are considered safe too.
* Integer numbers must still be exactly equal.
*/
function isSafeNumber(value, config) {
	if (isInteger(value)) return Number.isSafeInteger(Number.parseInt(value, 10));
	const parsed = String(Number.parseFloat(value));
	if (value === parsed) return true;
	const valueDigits = extractSignificantDigits(value);
	const parsedDigits = extractSignificantDigits(parsed);
	if (valueDigits === parsedDigits) return true;
	if (config?.approx === true) {
		const requiredDigits = 14;
		if (!isInteger(value) && parsedDigits.length >= requiredDigits && valueDigits.startsWith(parsedDigits.substring(0, requiredDigits))) return true;
	}
	return false;
}
var UnsafeNumberReason = /*#__PURE__*/ function(UnsafeNumberReason) {
	UnsafeNumberReason["underflow"] = "underflow";
	UnsafeNumberReason["overflow"] = "overflow";
	UnsafeNumberReason["truncate_integer"] = "truncate_integer";
	UnsafeNumberReason["truncate_float"] = "truncate_float";
	return UnsafeNumberReason;
}({});
/**
* When the provided value is an unsafe number, describe what the reason is:
* overflow, underflow, truncate_integer, or truncate_float.
* Returns undefined when the value is safe.
*/
function getUnsafeNumberReason(value) {
	if (isSafeNumber(value, { approx: false })) return;
	if (isInteger(value)) return UnsafeNumberReason.truncate_integer;
	const num = Number.parseFloat(value);
	if (!Number.isFinite(num)) return UnsafeNumberReason.overflow;
	if (num === 0) return UnsafeNumberReason.underflow;
	return UnsafeNumberReason.truncate_float;
}
/**
* Convert a string into a number when it is safe to do so.
* Throws an error otherwise, explaining the reason.
*/
function toSafeNumberOrThrow(value, config) {
	const number = Number.parseFloat(value);
	const unsafeReason = getUnsafeNumberReason(value);
	if (config?.approx === true ? unsafeReason && unsafeReason !== UnsafeNumberReason.truncate_float : unsafeReason) {
		const unsafeReasonText = unsafeReason?.replace(/_\w+$/, "");
		throw new Error(`Cannot safely convert to number: the value '${value}' would ${unsafeReasonText} and become ${number}`);
	}
	return number;
}
/**
* Split a number into sign, digits, and exponent.
* Leading zeros and non-canonical zeros are normalized.
*
* The value can be constructed again from a split number by inserting a dot
* at the second character of the digits if there is more than one digit,
* prepending it with the sign, and appending an "e" and the exponent as follows:
*
*     const reconstructed = `${sign}${digits[0]}.${digits.slice(1)}e${exponent}`
*
*/
function splitNumber(value) {
	const match = value.match(/^(-?)(\d+\.?\d*)([eE]([+-]?\d+))?$/);
	if (!match) throw new SyntaxError(`Invalid number: ${value}`);
	const sign = match[1];
	const digitsStr = match[2];
	let exponent = match[4] !== void 0 ? Number.parseInt(match[4], 10) : 0;
	const dot = digitsStr.indexOf(".");
	exponent += dot !== -1 ? dot - 1 : digitsStr.length - 1;
	const digits = digitsStr.replace(".", "").replace(/^0*/, (zeros) => {
		exponent -= zeros.length;
		return "";
	}).replace(/0*$/, "");
	return digits.length > 0 ? {
		sign,
		digits,
		exponent
	} : {
		sign: "",
		digits: "0",
		exponent: 0
	};
}
/**
* Compare two strings containing a numeric value
* Returns 1 when a is larger than b, 0 when they are equal,
* and -1 when a is smaller than b.
*/
function compareNumber(a, b) {
	if (a === b) return 0;
	const aa = splitNumber(a);
	const bb = splitNumber(b);
	const sign = aa.sign === "-" ? -1 : 1;
	if (aa.sign !== bb.sign) return sign;
	if (aa.exponent !== bb.exponent) return aa.exponent > bb.exponent ? sign : aa.exponent < bb.exponent ? -sign : 0;
	return aa.digits > bb.digits ? sign : aa.digits < bb.digits ? -sign : 0;
}
/**
* Count the significant digits of a number.
*
* For example:
*   '2.34' returns 3
*   '-77' returns 2
*   '0.003400' returns 2
*   '120.5e+30' returns 4
**/
function countSignificantDigits(value) {
	const { start, end } = getSignificantDigitRange(value);
	const dot = value.indexOf(".");
	if (dot === -1 || dot < start || dot > end) return end - start;
	return end - start - 1;
}
/**
* Get the significant digits of a number.
*
* For example:
*   '2.34' returns '234'
*   '-77' returns '77'
*   '0.003400' returns '34'
*   '120.5e+30' returns '1205'
**/
function extractSignificantDigits(value) {
	const { start, end } = getSignificantDigitRange(value);
	const digits = value.substring(start, end);
	const dot = digits.indexOf(".");
	if (dot === -1) return digits;
	return digits.substring(0, dot) + digits.substring(dot + 1);
}
/**
* Returns the range (start to end) of the significant digits of a value.
* Note that this range _may_ contain the decimal dot.
*
* For example:
*
*     getSignificantDigitRange('0.0325900') // { start: 3, end: 7 }
*     getSignificantDigitRange('2.0300')    // { start: 0, end: 3 }
*     getSignificantDigitRange('0.0')       // { start: 3, end: 3 }
*
*/
function getSignificantDigitRange(value) {
	let start = 0;
	if (value[0] === "-") start++;
	while (value[start] === "0" || value[start] === ".") start++;
	let end = value.lastIndexOf("e");
	if (end === -1) end = value.lastIndexOf("E");
	if (end === -1) end = value.length;
	while ((value[end - 1] === "0" || value[end - 1] === ".") && end > start) end--;
	return {
		start,
		end
	};
}
//#endregion
//#region node_modules/lossless-json/lib/esm/LosslessNumber.js
/**
* A lossless number. Stores its numeric value as string
*/
var LosslessNumber = class {
	isLosslessNumber = true;
	constructor(value) {
		if (!isNumber(value)) throw new Error(`Invalid number (value: "${value}")`);
		this.value = value;
	}
	/**
	* Get the value of the LosslessNumber as number or bigint.
	*
	* - a number is returned for safe numbers and decimal values that only lose some insignificant digits
	* - a bigint is returned for big integer numbers
	* - an Error is thrown for values that will overflow or underflow
	*
	* Note that you can implement your own strategy for conversion by just getting the value as string
	* via .toString(), and using util functions like isInteger, isSafeNumber, getUnsafeNumberReason,
	* and toSafeNumberOrThrow to convert it to a numeric value.
	*/
	valueOf() {
		const unsafeReason = getUnsafeNumberReason(this.value);
		if (unsafeReason === void 0 || unsafeReason === UnsafeNumberReason.truncate_float) return Number.parseFloat(this.value);
		if (isInteger(this.value)) return BigInt(this.value);
		throw new Error(`Cannot safely convert to number: the value '${this.value}' would ${unsafeReason} and become ${Number.parseFloat(this.value)}`);
	}
	/**
	* Get the value of the LosslessNumber as string.
	*/
	toString() {
		return this.value;
	}
};
/**
* Test whether a value is a LosslessNumber
*/
function isLosslessNumber(value) {
	return value && typeof value === "object" && value.isLosslessNumber || false;
}
/**
* Convert a number into a LosslessNumber if this is possible in a safe way
* If the value has too many digits, or is NaN or Infinity, an error will be thrown
*/
function toLosslessNumber(value) {
	if (countSignificantDigits(String(value)) > 15) throw new Error(`Invalid number: contains more than 15 digits and is most likely truncated and unsafe by itself (value: ${value})`);
	if (Number.isNaN(value)) throw new Error("Invalid number: NaN");
	if (!Number.isFinite(value)) throw new Error(`Invalid number: ${value}`);
	return new LosslessNumber(String(value));
}
/**
* Compare two lossless numbers.
* Returns 1 when a is larger than b, 0 when they are equal,
* and -1 when a is smaller than b.
*/
function compareLosslessNumber(a, b) {
	return compareNumber(a.value, b.value);
}
//#endregion
//#region node_modules/lossless-json/lib/esm/numberParsers.js
function parseLosslessNumber(value) {
	return new LosslessNumber(value);
}
function parseNumberAndBigInt(value) {
	return isInteger(value) ? BigInt(value) : Number.parseFloat(value);
}
//#endregion
//#region node_modules/lossless-json/lib/esm/revive.js
/**
* Revive a json object.
* Applies the reviver function recursively on all values in the JSON object.
* @param json   A JSON Object, Array, or value
* @param reviver
*              A reviver function invoked with arguments `key` and `value`,
*              which must return a replacement value. The function context
*              (`this`) is the Object or Array that contains the currently
*              handled value.
*/
function revive(json, reviver) {
	return reviveValue({ "": json }, "", json, reviver);
}
/**
* Revive a value
*/
function reviveValue(context, key, value, reviver) {
	if (Array.isArray(value)) return reviver.call(context, key, reviveArray(value, reviver));
	if (value && typeof value === "object" && !isLosslessNumber(value)) return reviver.call(context, key, reviveObject(value, reviver));
	return reviver.call(context, key, value);
}
/**
* Revive the properties of an object
*/
function reviveObject(object, reviver) {
	for (const key of Object.keys(object)) {
		const value = reviveValue(object, key, object[key], reviver);
		if (value !== void 0) object[key] = value;
		else delete object[key];
	}
	return object;
}
/**
* Revive the properties of an Array
*/
function reviveArray(array, reviver) {
	for (let i = 0; i < array.length; i++) array[i] = reviveValue(array, String(i), array[i], reviver);
	return array;
}
//#endregion
//#region node_modules/lossless-json/lib/esm/parse.js
/**
* The LosslessJSON.parse() method parses a string as JSON, optionally transforming
* the value produced by parsing.
*
* The parser is based on the parser of Tan Li Hou shared in
* https://lihautan.com/json-parser-with-javascript/
*
* @param text
* The string to parse as JSON. See the JSON object for a description of JSON syntax.
*
* @param [reviver]
* If a function, prescribes how the value originally produced by parsing is
* transformed, before being returned.
*
* @param [options=ParseOptions | NumberParserArgument]
* Pass a custom number parser. Input is a string, and the output can be unknown
* numeric value: number, bigint, LosslessNumber, or a custom BigNumber library.
*
* @returns Returns the Object corresponding to the given JSON text.
*
* @throws Throws a SyntaxError exception if the string to parse is not valid JSON.
*/
function parse(text, reviver, options) {
	const optionsObj = typeof options === "function" ? { parseNumber: options } : options;
	const parseNumber = optionsObj?.parseNumber ?? parseLosslessNumber;
	const onDuplicateKey = optionsObj?.onDuplicateKey ?? throwDuplicateKey;
	let i = 0;
	const value = parseValue();
	expectValue(value);
	expectEndOfInput();
	return reviver ? revive(value, reviver) : value;
	function parseObject() {
		if (text.charCodeAt(i) === codeOpeningBrace) {
			i++;
			skipWhitespace();
			const object = {};
			let initial = true;
			while (i < text.length && text.charCodeAt(i) !== codeClosingBrace) {
				if (!initial) {
					eatComma();
					skipWhitespace();
				} else initial = false;
				const start = i;
				const key = parseString();
				if (key === void 0) {
					throwObjectKeyExpected();
					return;
				}
				skipWhitespace();
				eatColon();
				const value = parseValue();
				if (value === void 0) {
					throwObjectValueExpected();
					return;
				}
				if (Object.prototype.hasOwnProperty.call(object, key) && !isDeepEqual(value, object[key])) {
					const returnedValue = onDuplicateKey({
						key,
						position: start + 1,
						oldValue: object[key],
						newValue: value
					});
					if (returnedValue !== void 0) object[key] = returnedValue;
				} else object[key] = value;
			}
			if (text.charCodeAt(i) !== codeClosingBrace) throwObjectKeyOrEndExpected();
			i++;
			return object;
		}
	}
	function parseArray() {
		if (text.charCodeAt(i) === codeOpeningBracket) {
			i++;
			skipWhitespace();
			const array = [];
			let initial = true;
			while (i < text.length && text.charCodeAt(i) !== codeClosingBracket) {
				if (!initial) eatComma();
				else initial = false;
				const value = parseValue();
				expectArrayItem(value);
				array.push(value);
			}
			if (text.charCodeAt(i) !== codeClosingBracket) throwArrayItemOrEndExpected();
			i++;
			return array;
		}
	}
	function parseValue() {
		skipWhitespace();
		const value = parseString() ?? parseNumeric() ?? parseObject() ?? parseArray() ?? parseKeyword("true", true) ?? parseKeyword("false", false) ?? parseKeyword("null", null);
		skipWhitespace();
		return value;
	}
	function parseKeyword(name, value) {
		if (text.slice(i, i + name.length) === name) {
			i += name.length;
			return value;
		}
	}
	function skipWhitespace() {
		while (isWhitespace(text.charCodeAt(i))) i++;
	}
	function parseString() {
		if (text.charCodeAt(i) === codeDoubleQuote) {
			i++;
			let result = "";
			while (i < text.length && text.charCodeAt(i) !== codeDoubleQuote) {
				if (text.charCodeAt(i) === codeBackslash) {
					const char = text[i + 1];
					const escapeChar = escapeCharacters[char];
					if (escapeChar !== void 0) {
						result += escapeChar;
						i++;
					} else if (char === "u") {
						if (isHex(text.charCodeAt(i + 2)) && isHex(text.charCodeAt(i + 3)) && isHex(text.charCodeAt(i + 4)) && isHex(text.charCodeAt(i + 5))) {
							result += String.fromCharCode(Number.parseInt(text.slice(i + 2, i + 6), 16));
							i += 5;
						} else throwInvalidUnicodeCharacter(i);
					} else throwInvalidEscapeCharacter(i);
				} else if (isValidStringCharacter(text.charCodeAt(i))) result += text[i];
				else throwInvalidCharacter(text[i]);
				i++;
			}
			expectEndOfString();
			i++;
			return result;
		}
	}
	function parseNumeric() {
		const start = i;
		if (text.charCodeAt(i) === codeMinus) {
			i++;
			expectDigit(start);
		}
		if (text.charCodeAt(i) === codeZero) i++;
		else if (isNonZeroDigit(text.charCodeAt(i))) {
			i++;
			while (isDigit(text.charCodeAt(i))) i++;
		}
		if (text.charCodeAt(i) === codeDot) {
			i++;
			expectDigit(start);
			while (isDigit(text.charCodeAt(i))) i++;
		}
		if (text.charCodeAt(i) === 101 || text.charCodeAt(i) === 69) {
			i++;
			if (text.charCodeAt(i) === codeMinus || text.charCodeAt(i) === codePlus) i++;
			expectDigit(start);
			while (isDigit(text.charCodeAt(i))) i++;
		}
		if (i > start) return parseNumber(text.slice(start, i));
	}
	function eatComma() {
		if (text.charCodeAt(i) !== codeComma) throw new SyntaxError(`Comma ',' expected after value ${gotAt()}`);
		i++;
	}
	function eatColon() {
		if (text.charCodeAt(i) !== codeColon) throw new SyntaxError(`Colon ':' expected after property name ${gotAt()}`);
		i++;
	}
	function expectValue(value) {
		if (value === void 0) throw new SyntaxError(`JSON value expected ${gotAt()}`);
	}
	function expectArrayItem(value) {
		if (value === void 0) throw new SyntaxError(`Array item expected ${gotAt()}`);
	}
	function expectEndOfInput() {
		if (i < text.length) throw new SyntaxError(`Expected end of input ${gotAt()}`);
	}
	function expectDigit(start) {
		if (!isDigit(text.charCodeAt(i))) {
			const numSoFar = text.slice(start, i);
			throw new SyntaxError(`Invalid number '${numSoFar}', expecting a digit ${gotAt()}`);
		}
	}
	function expectEndOfString() {
		if (text.charCodeAt(i) !== codeDoubleQuote) throw new SyntaxError(`End of string '"' expected ${gotAt()}`);
	}
	function throwObjectKeyExpected() {
		throw new SyntaxError(`Quoted object key expected ${gotAt()}`);
	}
	function throwDuplicateKey(_ref) {
		let { key, position } = _ref;
		throw new SyntaxError(`Duplicate key '${key}' encountered at position ${position}`);
	}
	function throwObjectKeyOrEndExpected() {
		throw new SyntaxError(`Quoted object key or end of object '}' expected ${gotAt()}`);
	}
	function throwArrayItemOrEndExpected() {
		throw new SyntaxError(`Array item or end of array ']' expected ${gotAt()}`);
	}
	function throwInvalidCharacter(char) {
		throw new SyntaxError(`Invalid character '${char}' ${pos()}`);
	}
	function throwInvalidEscapeCharacter(start) {
		const chars = text.slice(start, start + 2);
		throw new SyntaxError(`Invalid escape character '${chars}' ${pos()}`);
	}
	function throwObjectValueExpected() {
		throw new SyntaxError(`Object value expected after ':' ${pos()}`);
	}
	function throwInvalidUnicodeCharacter(start) {
		const chars = text.slice(start, start + 6);
		throw new SyntaxError(`Invalid unicode character '${chars}' ${pos()}`);
	}
	function pos() {
		return `at position ${i}`;
	}
	function got() {
		return i < text.length ? `but got '${text[i]}'` : "but reached end of input";
	}
	function gotAt() {
		return `${got()} ${pos()}`;
	}
}
function isWhitespace(code) {
	return code === codeSpace || code === codeNewline || code === codeTab || code === codeReturn;
}
function isHex(code) {
	return code >= codeZero && code <= codeNine || code >= 65 && code <= 70 || code >= 97 && code <= 102;
}
function isDigit(code) {
	return code >= codeZero && code <= codeNine;
}
function isNonZeroDigit(code) {
	return code >= codeOne && code <= codeNine;
}
function isValidStringCharacter(code) {
	return code >= 32 && code <= 1114111;
}
function isDeepEqual(a, b) {
	if (a === b) return true;
	if (Array.isArray(a) && Array.isArray(b)) return a.length === b.length && a.every((item, index) => isDeepEqual(item, b[index]));
	if (isObject(a) && isObject(b)) return [.../* @__PURE__ */ new Set([...Object.keys(a), ...Object.keys(b)])].every((key) => isDeepEqual(a[key], b[key]));
	return false;
}
function isObject(value) {
	return typeof value === "object" && value !== null;
}
var escapeCharacters = {
	"\"": "\"",
	"\\": "\\",
	"/": "/",
	b: "\b",
	f: "\f",
	n: "\n",
	r: "\r",
	t: "	"
};
var codeBackslash = 92;
var codeOpeningBrace = 123;
var codeClosingBrace = 125;
var codeOpeningBracket = 91;
var codeClosingBracket = 93;
var codeSpace = 32;
var codeNewline = 10;
var codeTab = 9;
var codeReturn = 13;
var codeDoubleQuote = 34;
var codePlus = 43;
var codeMinus = 45;
var codeZero = 48;
var codeOne = 49;
var codeNine = 57;
var codeComma = 44;
var codeDot = 46;
var codeColon = 58;
//#endregion
//#region node_modules/lossless-json/lib/esm/reviveDate.js
/**
* Revive a string containing an ISO 8601 date string into a JavaScript `Date` object
*/
function reviveDate(_key, value) {
	return typeof value === "string" && isoDateRegex.test(value) ? new Date(value) : value;
}
var isoDateRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/;
//#endregion
//#region node_modules/lossless-json/lib/esm/stringify.js
/**
* The LosslessJSON.stringify() method converts a JavaScript value to a JSON string,
* optionally replacing values if a replacer function is specified, or
* optionally including only the specified properties if a replacer array is specified.
*
* @param value
* The value to convert to a JSON string.
*
* @param [replacer]
* A function that alters the behavior of the stringification process,
* or an array of String and Number objects that serve as a whitelist for
* selecting the properties of the value object to be included in the JSON string.
* If this value is null or not provided, all properties of the object are
* included in the resulting JSON string.
*
* @param [space]
* A String or Number object that's used to insert white space into the output
* JSON string for readability purposes. If this is a Number, it indicates the
* number of space characters to use as white space; this number is capped at 10
* if it's larger than that. Values less than 1 indicate that no space should be
* used. If this is a String, the string (or the first 10 characters of the string,
* if it's longer than that) is used as white space. If this parameter is not
* provided (or is null), no white space is used.
*
* @param [numberStringifiers]
* An optional list with additional number stringifiers, for example to serialize
* a BigNumber. The output of the function must be valid stringified JSON.
* When `undefined` is returned, the property will be deleted from the object.
* The difference with using a `replacer` is that the output of a `replacer`
* must be JSON and will be stringified afterwards, whereas the output of the
* `numberStringifiers` is already stringified JSON.
*
* @returns Returns the string representation of the JSON object.
*/
function stringify(value, replacer, space, numberStringifiers) {
	const resolvedSpace = resolveSpace(space);
	return stringifyValue(typeof replacer === "function" ? replacer.call({ "": value }, "", value) : value, "");
	/**
	* Stringify a value
	*/
	function stringifyValue(value, indent) {
		if (Array.isArray(numberStringifiers)) {
			const stringifier = numberStringifiers.find((item) => item.test(value));
			if (stringifier) {
				const str = stringifier.stringify(value);
				if (typeof str !== "string" || !isNumber(str)) throw new Error(`Invalid JSON number: output of a number stringifier must be a string containing a JSON number (output: ${str})`);
				return str;
			}
		}
		if (typeof value === "boolean" || typeof value === "number" || typeof value === "string" || value === null || value instanceof Date || value instanceof Boolean || value instanceof Number || value instanceof String) return JSON.stringify(value);
		if (value?.isLosslessNumber) return value.toString();
		if (typeof value === "bigint") return value.toString();
		if (Array.isArray(value)) return stringifyArray(value, indent);
		if (value && typeof value === "object") return stringifyObject(value, indent);
	}
	/**
	* Stringify an array
	*/
	function stringifyArray(array, indent) {
		if (array.length === 0) return "[]";
		const childIndent = resolvedSpace ? indent + resolvedSpace : void 0;
		let str = resolvedSpace ? "[\n" : "[";
		for (let i = 0; i < array.length; i++) {
			const item = typeof replacer === "function" ? replacer.call(array, String(i), array[i]) : array[i];
			if (resolvedSpace) str += childIndent;
			if (typeof item !== "undefined" && typeof item !== "function") str += stringifyValue(item, childIndent);
			else str += "null";
			if (i < array.length - 1) str += resolvedSpace ? ",\n" : ",";
		}
		str += resolvedSpace ? `\n${indent}]` : "]";
		return str;
	}
	/**
	* Stringify an object
	*/
	function stringifyObject(object, indent) {
		if (typeof object.toJSON === "function") return stringify(object.toJSON(), replacer, space, void 0);
		const keys = Array.isArray(replacer) ? replacer.map(String) : Object.keys(object);
		if (keys.length === 0) return "{}";
		const childIndent = resolvedSpace ? indent + resolvedSpace : void 0;
		let first = true;
		let str = resolvedSpace ? "{\n" : "{";
		for (const key of keys) {
			const value = typeof replacer === "function" ? replacer.call(object, key, object[key]) : object[key];
			if (includeProperty(key, value)) {
				if (first) first = false;
				else str += resolvedSpace ? ",\n" : ",";
				const keyStr = JSON.stringify(key);
				str += resolvedSpace ? `${childIndent + keyStr}: ` : `${keyStr}:`;
				str += stringifyValue(value, childIndent);
			}
		}
		str += resolvedSpace ? `\n${indent}}` : "}";
		return str;
	}
	/**
	* Test whether to include a property in a stringified object or not.
	*/
	function includeProperty(_key, value) {
		return typeof value !== "undefined" && typeof value !== "function" && typeof value !== "symbol";
	}
}
/**
* Resolve a JSON stringify space:
* replace a number with a string containing that number of spaces
*/
function resolveSpace(space) {
	if (typeof space === "number") return " ".repeat(space);
	if (typeof space === "string" && space !== "") return space;
}
//#endregion
export { LosslessNumber, UnsafeNumberReason, compareLosslessNumber, compareNumber, config, getUnsafeNumberReason, isInteger, isLosslessNumber, isNumber, isSafeNumber, parse, parseLosslessNumber, parseNumberAndBigInt, reviveDate, splitNumber, stringify, toLosslessNumber, toSafeNumberOrThrow };
