CC       = gcc
CFLAGS   = -O3
LDFLAGS  = 
 
SUN_CPPFLAGS   = `sundials-config -m cvode -t s -l c -s cppflags`
SUN_LDFLAGS    = `sundials-config -m cvode -t s -l c -s libs`

SOURCES=$(wildcard *.c)
OBJECTS=${SOURCES:.c=.o}
EXECUTABLE=${SOURCES:.c=}

all: ${SOURCES} ${EXECUTABLE}

${EXECUTABLE}: ${OBJECTS} 
	${CC} ${LDFLAGS} -o $@ ${OBJECTS} ${SUN_LDFLAGS} 

.c.o:
	${CC} ${CFLAGS} ${SUN_CPPFLAGS} -c $< 

clean:
	rm -f ${OBJECTS} ${EXECUTABLE}
