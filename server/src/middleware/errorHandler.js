function errorHandler(err, req, res, next) {
  console.error('[Error Middleware]', err.stack || err.message);

  const statusCode = err.statusCode || 500;
  const clientResponse = {
    error: err.name || 'InternalServerError',
    message: err.message || 'An unexpected error occurred.',
  };

  if (err.darajaResponse) {
    clientResponse.details = err.darajaResponse.errorMessage || err.darajaResponse.ResponseDescription;
  }

  res.status(statusCode).json(clientResponse);
}

module.exports = errorHandler;
