--
-- Parameterisable N to M mux.
--

library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- For Modelsim
--use ieee.fixed_pkg.all;
--use ieee.fixed_float_types.ALL;

-- For ISE
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use IEEE.numeric_std.all;

entity ParamExp is
	generic( 
		BIT_TOP 	: integer := 20;	
		BIT_BOTTOM	: integer := -20);	
	port(
		clk		: In  Std_logic;
		init_model : in STD_LOGIC; --signal to all components to go into their init state
		Start		: In  Std_logic;
		X			: In sfixed(BIT_TOP downto BIT_BOTTOM);
		Done		: Out  Std_logic;
		Output	: Out sfixed(BIT_TOP downto BIT_BOTTOM)
		);
end ParamExp;

architecture RTL of ParamExp is
type MEM is array (0 to 7) of sfixed(BIT_TOP downto BIT_BOTTOM);
signal ISPOSITIVE : STD_LOGIC := '0';
signal ISGREATERTHANONE : STD_LOGIC := '0';
signal X_integer : sfixed(BIT_TOP downto 0);
signal X_fraction : sfixed(0 downto BIT_BOTTOM);
signal Output_int : sfixed(BIT_TOP downto BIT_BOTTOM);
signal Done_int : Std_logic;


signal output_fraction : sfixed(BIT_TOP downto BIT_BOTTOM);
signal output_fraction_next : sfixed(BIT_TOP downto BIT_BOTTOM);
signal current_term : sfixed(BIT_TOP downto BIT_BOTTOM);
signal current_term_next : sfixed(BIT_TOP downto BIT_BOTTOM);
signal COUNT_FRACTION : unsigned(3 downto 0);
signal COUNT_FRACTION_next : unsigned(3 downto 0);
signal DONEFRACTION : STD_LOGIC := '0';
signal DONEFRACTION_next  : STD_LOGIC := '0';


signal output_integer : sfixed(BIT_TOP downto BIT_BOTTOM);
signal output_integer_next : sfixed(BIT_TOP downto BIT_BOTTOM);
signal COUNT_INTEGER : unsigned(BIT_TOP+1 downto 0);
signal COUNT_INTEGER_next : unsigned(BIT_TOP+1 downto 0);
signal DONEINTEGER : STD_LOGIC := '0';
signal DONEINTEGER_next  : STD_LOGIC := '0';
signal E : sfixed(BIT_TOP downto BIT_BOTTOM) := to_sfixed(2.71828182845904523536028747135266249775724709369995,BIT_TOP,BIT_BOTTOM);
signal EInv : sfixed(BIT_TOP downto BIT_BOTTOM) := resize(reciprocal(to_sfixed(2.71828182845904523536028747135266249775724709369995,BIT_TOP,BIT_BOTTOM)),BIT_TOP,BIT_BOTTOM);
signal EMul : sfixed(BIT_TOP downto BIT_BOTTOM);


	signal n1: sfixed (BIT_TOP downto 0);
	signal n2: sfixed (n1'high + 1 downto n1'low);
	signal n3: ufixed (BIT_TOP + 1 downto 0);
begin

	splitUpXProcess: process(X,X_integer,ispositive,E,EInv)
	begin
		X_integer <= resize(abs(X) - 0.5,BIT_TOP,0);
		X_fraction <= resize(abs(X) - X_integer,0,BIT_BOTTOM);
		if To_slv ( resize ( X  ,BIT_TOP,BIT_BOTTOM))(BIT_TOP-BIT_BOTTOM)  = '0'  then
			ISPOSITIVE <= '1';
		else
			ISPOSITIVE <= '0';
		end if;
		if (ISPOSITIVE = '1') then
			EMul <= E;
		else
			EMul <= EInv;
		end if;
	end process splitUpXProcess;
	

   fractionCombProcess: process(COUNT_FRACTION,Start,output_fraction,current_term,X_fraction,ISPOSITIVE,current_term_next,init_model)
		variable MEM8Xsfixed : MEM := (to_sfixed (1,BIT_TOP, BIT_BOTTOM),to_sfixed (0.5,BIT_TOP, BIT_BOTTOM),to_sfixed (0.33333333,BIT_TOP, BIT_BOTTOM),to_sfixed (0.25,BIT_TOP, BIT_BOTTOM),
		to_sfixed (0.2,BIT_TOP, BIT_BOTTOM),to_sfixed (0.16666666667,BIT_TOP, BIT_BOTTOM),to_sfixed (0.142857142857,BIT_TOP, BIT_BOTTOM),to_sfixed (0.125,BIT_TOP, BIT_BOTTOM));
		begin
			output_fraction_next <= output_fraction;
			COUNT_FRACTION_next <= COUNT_FRACTION;
			current_term_next <= current_term;
			DONEFRACTION_next  <= '0';
			current_term_next <= resize(MEM8Xsfixed(to_integer(unsigned(COUNT_FRACTION(2 downto 0)))) * 
				resize(X_fraction * current_term,BIT_TOP, BIT_BOTTOM),BIT_TOP, BIT_BOTTOM);
			if init_model = '1' then
				DONEFRACTION_next  <= '1';
				COUNT_FRACTION_next <= "1001"; 	
				output_fraction_next <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
				current_term_next <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
			else
				if Start = '1' then
					DONEFRACTION_next  <= '0';
					COUNT_FRACTION_next <= "0000"; 	
					output_fraction_next <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
					current_term_next <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
				elsif COUNT_FRACTION = "1001" then  
					DONEFRACTION_next  <= '1';
					current_term_next <= current_term;
				else
					DONEFRACTION_next  <= '0';
					if (ISPOSITIVE = '1') then
						output_fraction_next <= resize(output_fraction + current_term_next,BIT_TOP, BIT_BOTTOM);
					else
						if (COUNT_FRACTION(0) = '0') then
							output_fraction_next <= resize(output_fraction - current_term_next,BIT_TOP, BIT_BOTTOM);
						else
							output_fraction_next <= resize(output_fraction + current_term_next,BIT_TOP, BIT_BOTTOM);
						end if;
					end if;
					COUNT_FRACTION_next <= COUNT_FRACTION + 1;
				end if; 
			end if; 
		end process fractionCombProcess;

	fractionSynProcess: process(clk)
		variable Sel : integer;
		begin 
			if clk'event and clk = '1' then
				output_fraction <= output_fraction_next;
				COUNT_FRACTION <= COUNT_FRACTION_next;
				current_term <= current_term_next;
				DONEFRACTION <= DONEFRACTION_next;
				--report "The value of output_fraction = " & real'image(to_real(output_fraction)) & " and current_term " & 
				--	real'image(to_real(current_term));
			end if;
	end process fractionSynProcess;


	
	
	integerCombProcess: process(COUNT_INTEGER,output_integer,x_integer,Start,EMul,init_model)
	
		begin
		DONEINTEGER_next <= '0';
		COUNT_INTEGER_next <= COUNT_INTEGER;
		output_integer_next <= output_integer;
		if init_model = '1' then
			DONEINTEGER_next <= '0';
			COUNT_INTEGER_next <= to_unsigned(0,COUNT_INTEGER_next'length); 	
			output_integer_next <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
		else
			if Start = '1' then
				DONEINTEGER_next <= '0';
				COUNT_INTEGER_next <= unsigned(ufixed(abs(X_integer))); 	
				output_integer_next <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
			else
				if COUNT_INTEGER = 0 then 
					DONEINTEGER_next <= '1';
					COUNT_INTEGER_next <= COUNT_INTEGER;
					output_integer_next <= output_integer;
				else
					DONEINTEGER_next <= '0'; 
					output_integer_next <= resize(output_integer * EMul,BIT_TOP,BIT_BOTTOM);
					COUNT_INTEGER_next <= COUNT_INTEGER - 1;
				end if;
			end if; 
		end if; 
	end process integerCombProcess;
	
	
	
	integerSynProcess: process(clk,x_integer,count_integer,output_integer)
		begin
		COUNT_INTEGER <= COUNT_INTEGER;
		output_integer <= output_integer;
		if clk = '1' and clk'event then
			COUNT_INTEGER <= COUNT_INTEGER_next;
			output_integer <= output_integer_next;
			DONEINTEGER <= DONEINTEGER_next;
		end if;
	end process integerSynProcess;

	outputCombProcess: process(output_fraction,output_integer,DONEINTEGER,DONEFRACTION)
	begin
		Output_int <=  resize(output_fraction * output_integer,BIT_TOP, BIT_BOTTOM);
		if DONEFRACTION = '1' and DONEINTEGER = '1' then
			Done_int <= '1';
		else
			Done_int <= '0';
		end if;
		
	end process outputCombProcess;
	
	Done <= Done_int;
	Output <= Output_int;
	
	--process (DONEFRACTION) 	
	--begin
	--	if (DONEFRACTION'event or DONEINTEGER'event) and DONEFRACTION = '1' and DONEINTEGER = '1' then
	--		report "The value of X_integer = " & real'image(to_real(X_integer)) & " and X_fraction " & real'image(to_real(X_fraction));
	--		report "The value of exp( " & real'image(to_real(X)) & " ) = " & 
	--			real'image(to_real(output_integer)) & " * " & real'image(to_real(output_fraction));
	--	end if;
	--end process;
		
end RTL; 
