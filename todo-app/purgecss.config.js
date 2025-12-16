module.exports = {
    content: [
        'src/main/resources/templates/**/*.html',
        'src/main/resources/static/js/**/*.js'
    ],
    css: ['src/main/resources/static/css/fontawesome.css'],
    output: 'src/main/resources/static/css/',
    safelist: {
        // Keep all fa-* classes since they might be dynamically added
        standard: [/^fa-/, /^fas$/, /^far$/, /^fab$/],
        deep: [],
        greedy: []
    },
    defaultExtractor: content => content.match(/[\w-/:]+(?<!:)/g) || []
};
