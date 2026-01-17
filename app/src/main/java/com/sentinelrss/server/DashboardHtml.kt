package com.sentinelrss.server

object DashboardHtml {
    fun getHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SentinelRSS</title>
                <style>
                    body { background-color: #121212; color: #e0e0e0; font-family: sans-serif; }
                    .container { max-width: 800px; margin: 0 auto; padding: 20px; }
                    .article { background-color: #1e1e1e; padding: 15px; margin-bottom: 10px; border-radius: 5px; }
                    .article h2 { margin-top: 0; }
                    a { color: #bb86fc; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>SentinelRSS Dashboard</h1>
                    <div id="articles">Loading...</div>
                </div>
                <script>
                    fetch('/api/articles')
                        .then(response => {
                            if (!response.ok) {
                                return response.text().then(text => { throw new Error(text || response.statusText) });
                            }
                            return response.json();
                        })
                        .then(data => {
                            const container = document.getElementById('articles');
                            if (data.length === 0) {
                                container.innerHTML = '<p>No articles found. Check backend logs.</p>';
                                return;
                            }
                            container.innerHTML = '';
                            data.forEach(article => {
                                const div = document.createElement('div');
                                div.className = 'article';
                                div.innerHTML = `
                                    <h2><a href="${'$'}{article.link}" target="_blank">${'$'}{article.title}</a></h2>
                                    <p>${'$'}{article.description}</p>
                                    <small>Score: ${'$'}{article.score}</small>
                                    <button onclick="likeArticle(${'$'}{article.id})">Like</button>
                                `;
                                container.appendChild(div);
                            });
                        })
                        .catch(err => {
                            document.getElementById('articles').innerHTML = '<p style="color:red">Error loading articles: ' + err.message + '</p>';
                        });

                    function likeArticle(id) {
                        fetch(`/api/articles/${'$'}{id}/like`, { method: 'POST' })
                            .then(response => {
                                if (response.ok) alert('Article liked! Engine will learn.');
                                else alert('Failed to like.');
                            });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
