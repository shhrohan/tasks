/**
 * PurgeCSS script for Font Awesome
 * Removes unused CSS from fontawesome.css based on actual usage in HTML/JS
 */
const { PurgeCSS } = require('purgecss');
const fs = require('fs');

const cssPath = 'src/main/resources/static/css/fontawesome.css';
const outputPath = 'src/main/resources/static/css/fontawesome.purged.css';

async function purgeFontAwesome() {
    console.log('🚀 Running PurgeCSS on Font Awesome...');

    const contentFiles = [
        'src/main/resources/templates/index.html',
        'src/main/resources/templates/login.html',
        'src/main/resources/templates/register.html',
        'src/main/resources/static/js/app.js',
        'src/main/resources/static/js/modules/store.js',
        'src/main/resources/static/js/modules/api.js',
        'src/main/resources/static/js/modules/drag.js'
    ].filter(f => fs.existsSync(f));

    try {
        const result = await new PurgeCSS().purge({
            content: contentFiles,
            css: [cssPath],
            // Only safelist the base classes, let PurgeCSS detect which fa-* icons are actually used
            safelist: ['fas', 'far', 'fab', 'fa-solid', 'fa-regular', 'fa-brands']
        });

        if (result.length > 0) {
            const purgedCSS = result[0].css;
            fs.writeFileSync(outputPath, purgedCSS);

            const originalSize = fs.statSync(cssPath).size;
            const purgedSize = purgedCSS.length;
            const savings = ((originalSize - purgedSize) / originalSize * 100).toFixed(1);

            console.log(`✅ Done! Original: ${(originalSize / 1024).toFixed(1)}KB → Purged: ${(purgedSize / 1024).toFixed(1)}KB (${savings}% smaller)`);
            console.log(`📁 Output: ${outputPath}`);
        } else {
            console.error('❌ PurgeCSS returned no results');
        }
    } catch (err) {
        console.error('❌ Error:', err.message);
    }
}

purgeFontAwesome();
