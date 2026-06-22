/**
 * Tước bỏ dấu tiếng Việt (ví dụ: "giường" -> "giuong")
 */
function stripDiacritics(text) {
    if (!text) return "";
    return text.normalize('NFD')
               .replace(/[\u0300-\u036f]/g, "")
               .replace(/đ/g, "d")
               .replace(/Đ/g, "D")
               .toLowerCase();
}

/**
 * Tạo regex khớp với các ký tự tiếng Việt có dấu hoặc không dấu.
 * Ví dụ: "giuong" hoặc "giường" đều -> "g[iìíỉĩị][uùúủũụưừứửữự][oòóỏõọôồốổỗộơờớởỡợ]ng"
 */
function createDiacriticRegex(keyword) {
    if (!keyword) return "";

    // Đưa từ khóa về dạng không dấu trước khi tạo Regex
    const cleanKw = stripDiacritics(keyword);

    const charMap = {
        'a': '[aàáảãạăằắẳẵặâầấẩẫậ]',
        'd': '[dđ]',
        'e': '[eèéẻẽẹêềếểễệ]',
        'i': '[iìíỉĩị]',
        'o': '[oòóỏõọôồốổỗộơờớởỡợ]',
        'u': '[uùúủũụưừứửữự]',
        'y': '[yỳýỷỹỵ]'
    };

    return cleanKw.split('').map(char => charMap[char] || char).join('');
}

module.exports = { createDiacriticRegex, stripDiacritics };
